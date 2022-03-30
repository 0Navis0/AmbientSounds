package team.creative.ambientsounds;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.common.Mod;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.ICreativeLoader;
import team.creative.creativecore.client.ClientLoader;
import team.creative.creativecore.client.CreativeCoreClient;

@Mod(value = AmbientSounds.MODID)
public class AmbientSounds implements ClientLoader {
    
    public static final Logger LOGGER = LogManager.getLogger(AmbientSounds.MODID);
    public static final String MODID = "ambientsounds";
    public static final AmbientSoundsConfig CONFIG = new AmbientSoundsConfig();
    public static AmbientTickHandler TICK_HANDLER;
    
    public AmbientSounds() {
        ICreativeLoader loader = CreativeCore.loader();
        loader.registerClient(this);
    }
    
    public static void reload() {
        if (TICK_HANDLER.engine != null)
            TICK_HANDLER.engine.stopEngine();
        if (TICK_HANDLER.enviroment != null)
            TICK_HANDLER.enviroment.reload();
        TICK_HANDLER.setEngine(AmbientEngine.loadAmbientEngine(TICK_HANDLER.soundEngine));
    }
    
    @Override
    public void onInitializeClient() {
        ICreativeLoader loader = CreativeCore.loader();
        loader.registerDisplayTest(() -> loader.ignoreServerNetworkConstant(), (a, b) -> true);
        
        TICK_HANDLER = new AmbientTickHandler();
        loader.registerClientTick(TICK_HANDLER::onTick);
        loader.registerClientRender(TICK_HANDLER::onRender);
        loader.registerLoadLevel(TICK_HANDLER::loadLevel);
        
        loader.registerClientStarted(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ReloadableResourceManager reloadableResourceManager = (ReloadableResourceManager) minecraft.getResourceManager();
            
            reloadableResourceManager.registerReloadListener(new PreparableReloadListener() {
                
                @Override
                public CompletableFuture<Void> reload(PreparationBarrier p_10638_, ResourceManager p_10639_, ProfilerFiller p_10640_, ProfilerFiller p_10641_, Executor p_10642_, Executor p_10643_) {
                    return CompletableFuture.runAsync(AmbientSounds::reload, p_10643_);
                }
            });
        });
        
        CreativeCoreClient.registerClientConfig(MODID);
    }
    
    @Override
    public void registerClientCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("ambient-debug").executes(x -> {
            TICK_HANDLER.showDebugInfo = !TICK_HANDLER.showDebugInfo;
            return Command.SINGLE_SUCCESS;
        }));
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("ambient-reload").executes(x -> {
            AmbientSounds.reload();
            return Command.SINGLE_SUCCESS;
        }));
    }
    
}
