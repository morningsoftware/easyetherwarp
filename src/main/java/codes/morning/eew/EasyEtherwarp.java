package codes.morning.eew;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyEtherwarp implements ClientModInitializer {

	public static boolean ENABLED;
	public static final String MOD_ID = "easyetherwarp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private enum EtherwarpState {
		IDLE,
		SNEAKING,
		INTERACTING
	}

	private EtherwarpState state = EtherwarpState.IDLE;

	@Override
	public void onInitializeClient() {
		ENABLED = true;
		LOGGER.info("Starting EEW!");

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				registerCommand(dispatcher));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ENABLED) return;
			onClientTick(client);
		});
	}

	public static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal("eew")
				.executes(context -> {
					ENABLED = !ENABLED;
					String status = ENABLED ? "Enabled" : "Disabled";
					LOGGER.info("[EEW] " + status + ".");

					MinecraftClient client = MinecraftClient.getInstance();
					if (client.player != null) {
						client.player.sendMessage(
								Text.literal("[EEW] " + status + "."),
								false
						);
					}
					return 1;
				})
		);
	}

	private void onClientTick(MinecraftClient client) {
		if (client.player == null) return;

		// run the tick function every game tick
		tick(client);

		if (state == EtherwarpState.IDLE) {
			ItemStack held = client.player.getMainHandStack();

			// check if criteria for tp is filled
			if (isAspectOfTheVoid(held) && client.options.attackKey.isPressed()) {
				client.options.sneakKey.setPressed(true);
				state = EtherwarpState.SNEAKING;
				LOGGER.debug("[EEW] SNEAKING");
			}
		}
	}

	private void tick(MinecraftClient client) {
		switch (state) {
			// if sneaking, do the interact
			case SNEAKING -> {
				client.interactionManager.interactItem(
						client.player,
						client.player.getActiveHand()
				);
				state = EtherwarpState.INTERACTING;
				LOGGER.debug("[EEW] INTERACTING");
			}
			// if interacted, release sneak
			case INTERACTING -> {
				client.options.sneakKey.setPressed(false);
				state = EtherwarpState.IDLE;
				LOGGER.debug("[EEW] IDLE");
			}
			case IDLE -> {
				// just vibe
			}
		}
	}

	private boolean isAspectOfTheVoid(ItemStack stack) {
		// not sure if this breaks! prolly fine
		if (stack.isEmpty()) return false;
		return stack.getName().getString().contains("Aspect of the Void");
	}
}