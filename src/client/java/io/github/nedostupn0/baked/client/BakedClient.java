package io.github.nedostupn0.baked.client;

import io.github.nedostupn0.baked.client.compat.ModCompat;
import io.github.nedostupn0.baked.client.debug.DebugCommand;
import io.github.nedostupn0.baked.client.model.TintPalette;
import io.github.nedostupn0.baked.client.config.configscreen.BakedConfigScreen;
import io.github.nedostupn0.baked.client.platform.PlatformHelper;
import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.resources.loader.SkullPackLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class BakedClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Registry.init();
		ModCompat.init();
		BakedConfigScreen.registerCommand();
		DebugCommand.register();
		TintPalette.register();
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.tryParse("baked:skulls"), new SkullPackLoader());
		PlatformHelper.registerPlatformEvents();
	}
}