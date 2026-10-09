package io.github.nedostupn0.baked.client.config.configscreen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
        @Override
        public ConfigScreenFactory<BakedConfigScreen> getModConfigScreenFactory() {
                return BakedConfigScreen::new;
        }
}