package com.sange.ender_bags.compat.clientsort;

import com.sange.ender_bags.EnderBags;
import com.sange.ender_bags.container.EnderBagMenu;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Policy;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import java.util.TreeSet;

/**
 * Isolated behind a ModList check in EnderBagsClient so ClientSort remains an optional runtime
 * dependency. This class must never be referenced from common/server initialization.
 */
public final class ClientSortCompat {
    private ClientSortCompat() {
    }

    public static void registerDefaultPolicy() {
        Config.Options options = Config.options();
        String key = ClassPolicy.getKey(EnderBagMenu.class.getName(), null);

        // Never replace a policy the player has already customized.
        if (options.classPolicies.containsKey(key)) {
            return;
        }

        options.classPolicies.put(
                key,
                new ClassPolicy(
                        EnderBagMenu.class.getName(),
                        null,
                        null,
                        false,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        null,
                        false,
                        new TreeSet<>()));
        PolicyManager.reloadPolicyClasses(options.classPolicies.keySet());
        EnderBags.LOGGER.info("Enabled ClientSort compatibility for Ender Bag menus");
    }
}
