package com.dbrighthd.regexmod.mixin.client;

import com.dbrighthd.regexmod.cache.RegexCache;
import com.dbrighthd.regexmod.selector.RegexModelSelector;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModel.BakingContext;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel.SwitchCase;
import net.minecraft.client.renderer.item.SelectItemModel.UnbakedSwitch;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Mixin(UnbakedSwitch.class)
public class UnbakedSwitchMixin {

	@Shadow private List<SwitchCase<?>> cases;
	@Shadow private SelectItemModelProperty<?>   property;

	@Inject(method = "bake", at = @At("HEAD"), cancellable = true)
	private <T> void onBake(BakingContext context,
							Matrix4fc transformation,
							ItemModel fallback,
							CallbackInfoReturnable<ItemModel> cir) {

		//check if no regex markers exist, let vanilla run untouched. should fix broken banner shields
		boolean hasRegex = false;
		for (SwitchCase<?> sc0 : this.cases) {
			for (Object v0 : sc0.values()) {
				if (v0 instanceof Component t) {
					String raw = t.getString();
					if (raw.startsWith("regex:") || raw.startsWith("iregex:")
							|| raw.startsWith("pattern:") || raw.startsWith("ipattern:")) {
						hasRegex = true;
						break;
					}
				}
			}
			if (hasRegex) break;
		}

		if (!hasRegex) {
			return; //I can't believe I forgot to do this initially
		}
		Object2ObjectOpenHashMap<T, ItemModel> exactMatches = new Object2ObjectOpenHashMap<>();
		List<Pair<Pattern, ItemModel>> regexCases = new ArrayList<>();

		for (SwitchCase<T> sc : (List<SwitchCase<T>>) (Object) this.cases) {
			ItemModel baked = sc.model().bake(context, transformation);

			for (T val : sc.values()) {
				if (val instanceof Component tVal) {
					String raw = tVal.getString();

					if (raw.startsWith("regex:")) {
						regexCases.add(Pair.of(
								RegexCache.getOrCompilePattern(raw.substring("regex:".length()), 0),
								baked));
					} else if (raw.startsWith("iregex:")) {
						regexCases.add(Pair.of(
								RegexCache.getOrCompilePattern(raw.substring("iregex:".length()),
										Pattern.CASE_INSENSITIVE),
								baked));
					} else if (raw.startsWith("pattern:")) {
						String pat = raw.substring("pattern:".length())
								.replace("*", ".*").replace("?", ".");
						regexCases.add(Pair.of(
								RegexCache.getOrCompilePattern(pat, 0),
								baked));
					} else if (raw.startsWith("ipattern:")) {
						String pat = raw.substring("ipattern:".length())
								.replace("*", ".*").replace("?", ".");
						regexCases.add(Pair.of(
								RegexCache.getOrCompilePattern(pat, Pattern.CASE_INSENSITIVE),
								baked));
					} else {
						exactMatches.put(val, baked);   // plain value
					}
				} else {
					exactMatches.put(val, baked);
				}
			}
		}

		exactMatches.defaultReturnValue(fallback);

		@SuppressWarnings("unchecked")
		SelectItemModelProperty<T> prop = (SelectItemModelProperty<T>) this.property;

		cir.setReturnValue(new SelectItemModel<>(prop,
				new RegexModelSelector<>(exactMatches, regexCases, fallback)));
	}
}
