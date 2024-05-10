package team.creative.ambientsounds.environment;

import java.util.HashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import team.creative.ambientsounds.condition.AmbientVolume;
import team.creative.ambientsounds.dimension.AmbientDimension;
import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.mod.SereneSeasonsCompat;
import team.creative.creativecore.client.render.text.DebugTextRenderer;

public class AmbientEnvironment {
    
    public AmbientDimension dimension;
    
    public boolean muted = false;
    
    public boolean night;
    /** 0: night, 1: day ... has smooth transition */
    public double time;
    
    public double rainSurfaceVolume;
    public boolean raining;
    public boolean snowing;
    public boolean thundering;
    
    public BiomeEnvironment biome = new BiomeEnvironment();
    public TerrainEnvironment terrain = new TerrainEnvironment();
    public EntityEnvironment entity = new EntityEnvironment();
    
    public AmbientVolume biomeVolume = AmbientVolume.SILENT;
    
    public HashMap<String, AmbientVolume> biomeTypeVolumes = new HashMap<>();
    
    public double absoluteHeight;
    public double relativeHeight;
    public double relativeMinHeight;
    public double relativeMaxHeight;
    public double underwater;
    
    public double temperature;
    
    public AmbientEnvironment() {}
    
    public boolean isRainAudibleAtSurface() {
        return rainSurfaceVolume > 0;
    }
    
    public void analyzeFast(AmbientDimension dimension, Player player, Level level, float deltaTime) {
        this.dimension = dimension;
        this.raining = level.isRainingAt(player.blockPosition().above());
        this.snowing = level.getBiome(player.blockPosition()).value().coldEnoughToSnow(player.blockPosition()) && level.isRaining();
        this.thundering = level.isThundering() && !snowing;
        
        this.absoluteHeight = player.getEyeY();
        this.relativeHeight = absoluteHeight - terrain.averageHeight;
        this.relativeMinHeight = absoluteHeight - terrain.minHeight;
        this.relativeMaxHeight = absoluteHeight - terrain.maxHeight;
        
        this.temperature = SereneSeasonsCompat.getTemperature(player);
        
        analyzeUnderwater(player, level);
        analyzeTime(level, deltaTime);
        entity.analyzeFast(dimension, player, level, deltaTime);
    }
    
    public void analyzeTime(Level level, float deltaTime) {
        double sunAngle = Math.toDegrees(level.getSunAngle(deltaTime));
        this.night = sunAngle > 90 && sunAngle < 270;
        double fadeTime = 10;
        if (sunAngle > 90 - fadeTime && sunAngle < 90 + fadeTime)
            this.time = Math.min((sunAngle - (90 - fadeTime)) / (fadeTime * 2), 1);
        else if (sunAngle > 270 - fadeTime && sunAngle < 270 + fadeTime)
            this.time = Math.max(1 - ((sunAngle - (270 - fadeTime)) / (fadeTime * 2)), 0);
        else if (night)
            this.time = 0;
        else
            this.time = 1;
    }
    
    public void analyzeUnderwater(Player player, Level level) {
        int depth = 0;
        if (player.isEyeInFluid(FluidTags.WATER)) {
            BlockPos blockpos = BlockPos.containing(player.getEyePosition());
            while (level.getFluidState(blockpos).is(FluidTags.WATER)) {
                depth++;
                blockpos = blockpos.above();
            }
        }
        this.underwater = depth;
    }
    
    public void analyzeSlow(AmbientDimension dimension, AmbientEngine engine, Player player, Level level, float deltaTime) {
        terrain.analyze(engine, dimension, player, level);
        biome = new BiomeEnvironment(engine, player, level, biomeVolume);
        rainSurfaceVolume = biome.rainVolume();
    }
    
    public void collectLevelDetails(DebugTextRenderer text) {
        text.detail("dimension", dimension);
        text.detail("night", night);
        text.detail("rain", raining);
        text.detail("rainSurfaceVolume", rainSurfaceVolume);
        text.detail("snow", snowing);
        text.detail("storm", thundering);
        text.detail("time", time);
        
    }
    
    public void collectPlayerDetails(DebugTextRenderer text, Player player) {
        text.detail("underwater", underwater);
        text.detail("temp", temperature);
        text.detail("height", "r:" + DebugTextRenderer.DECIMAL_FORMAT.format(relativeHeight) + ",a:" + DebugTextRenderer.DECIMAL_FORMAT.format(
            terrain.averageHeight) + " (" + DebugTextRenderer.DECIMAL_FORMAT.format(relativeMinHeight) + "," + DebugTextRenderer.DECIMAL_FORMAT.format(relativeMaxHeight) + ")");
        
    }
    
    public void collectTerrainDetails(DebugTextRenderer text) {
        terrain.collectDetails(text);
    }
    
    public void collectBiomeDetails(DebugTextRenderer text) {
        text.detail("b-volume", biomeVolume);
        biome.collectDetails(text);
    }
    
    public void reload() {
        terrain.scanner = null;
    }
    
}