package com.dbrighthd.regexmod.mixin.client;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.render.item.model.SelectItemModel.UnbakedSwitch;
import net.minecraft.client.render.item.model.SelectItemModel.SwitchCase;
import net.minecraft.client.render.item.model.SelectItemModel.ModelSelector;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.ItemModel.BakeContext;
import net.minecraft.client.render.item.property.select.SelectProperty;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Mixin(UnbakedSwitch.class)
public class UnbakedSwitchMixin {
	@Shadow private List<SwitchCase<?>> cases;
	@Shadow private SelectProperty<?> property;

	@Inject(
			method = "bake",
			at = @At("HEAD"),
			cancellable = true
	)
	private <T> void onBake(BakeContext context, ItemModel fallback, CallbackInfoReturnable<ItemModel> cir) {

		Object2ObjectOpenHashMap<T, ItemModel> exactMatches = new Object2ObjectOpenHashMap<>(); //didnt let me use a regular map
		List<Pair<Pattern, ItemModel>> regexCases = new ArrayList<>();

		for (SwitchCase<T> sc : (List<SwitchCase<T>>) (Object) this.cases) {
			ItemModel baked = sc.model().bake(context);

			for (T val : sc.values()) {
				//"god I wish there was an easier way to do this"
				if (val instanceof Text tVal && tVal.getString().startsWith("regex:")) {
					String pat = tVal.getString().substring("regex:".length());
					regexCases.add(Pair.of(Pattern.compile(pat), baked));
				} else if (val instanceof Text tVal && tVal.getString().startsWith("iregex:")) {
					String pat = tVal.getString().substring("iregex:".length());
					regexCases.add(Pair.of(Pattern.compile(pat, Pattern.CASE_INSENSITIVE), baked));
				} else if (val instanceof Text tVal && tVal.getString().startsWith("pattern:")) {
					String pat = tVal.getString().substring("pattern:".length()).replace("*",".*").replace("?",".");
					regexCases.add(Pair.of(Pattern.compile(pat), baked));
				} else if (val instanceof Text tVal && tVal.getString().startsWith("ipattern:")) {
					String pat = tVal.getString().substring("ipattern:".length()).replace("*",".*").replace("?",".");
					regexCases.add(Pair.of(Pattern.compile(pat, Pattern.CASE_INSENSITIVE), baked));
				}
				else {
					exactMatches.put(val, baked);
				}
			}
		}
		exactMatches.defaultReturnValue(fallback);
		ModelSelector<T> selector = (value, world) -> {
			ItemModel m = exactMatches.get(value);
			if (m != fallback) return m;
			if (value instanceof Text tv) {
				String s = tv.getString();
				for (Pair<Pattern, ItemModel> rc : regexCases) {
					if (rc.getFirst().matcher(s).matches()) {
						return rc.getSecond();
					}
				}
			}

			return fallback;
		};

		@SuppressWarnings("unchecked")
		SelectProperty<T> prop = (SelectProperty<T>)(Object) this.property;

		cir.setReturnValue(new SelectItemModel<>(prop, selector));
	}
}