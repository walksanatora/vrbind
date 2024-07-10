package net.walksanator.vrbind.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.walksanator.vrbind.mixin.client.KeyBindingAccessor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

public class VrRebindingClient implements ClientModInitializer {

    private static Logger logger = LoggerFactory.getLogger("vrbind_client");

    private static KeyBinding keyBinding;
    private static TagKey<Item> staveTag = TagKey.of(Registry.ITEM.getKey(), Identifier.of("hexcasting","staves"));
    private static boolean toggle = false;
    private static KeyBinding rightHandAdvanced;
    private static KeyBinding leftHandAdvanced;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vrbind.force", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_K, // The keycode of the key
                "category.vrbind.test" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                toggle = !toggle;
                client.player.sendMessage(Text.translatable("vrbind.toggled"), false);
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            ClientPlayerEntity player = client.player;
            if (client.player == null) {
                return;
            }
            boolean right = player.getStackInHand(Hand.MAIN_HAND).isIn(staveTag) || toggle;
            boolean left = player.getStackInHand(Hand.OFF_HAND).isIn(staveTag) || toggle;
            if (right ^ client.options.attackKey.isUnbound()) {
                logger.info("toggling casting on right hand (r/bound) {} {}",right,client.options.attackKey.isUnbound());
                if (rightHandAdvanced == null) {
                    rightHandAdvanced = Arrays.stream(client.options.allKeys)
                            .filter((it) -> Objects.equals(it.getTranslationKey(), "key.hex_vr.draw_spell_r_advanced")).findFirst()
                            .get();
                }
                if (right) {
                    logger.info("enabling right-hand key");
                    InputUtil.Key key = ((KeyBindingAccessor)client.options.attackKey).getBoundKey();
                    client.options.attackKey.setBoundKey(InputUtil.UNKNOWN_KEY);
                    rightHandAdvanced.setBoundKey(key);
                } else {
                    logger.info("disabling right-hand key");
                    InputUtil.Key key = ((KeyBindingAccessor)rightHandAdvanced).getBoundKey();
                    rightHandAdvanced.setBoundKey(InputUtil.UNKNOWN_KEY);
                    client.options.attackKey.setBoundKey(key);
                }
            }

            if (left ^ client.options.useKey.isUnbound()) {
                if (leftHandAdvanced == null) {
                    leftHandAdvanced = Arrays.stream(client.options.allKeys)
                            .filter((it) -> Objects.equals(it.getTranslationKey(), "key.hex_vr.draw_spell_l_advanced")).findFirst()
                            .get();
                }
                if (left) {
                    InputUtil.Key key = ((KeyBindingAccessor)client.options.useKey).getBoundKey();
                    client.options.useKey.setBoundKey(InputUtil.UNKNOWN_KEY);
                    leftHandAdvanced.setBoundKey(key);
                } else {
                    InputUtil.Key key = ((KeyBindingAccessor)leftHandAdvanced).getBoundKey();
                    leftHandAdvanced.setBoundKey(InputUtil.UNKNOWN_KEY);
                    client.options.useKey.setBoundKey(key);
                }
            }
        });
    }
}
