package team.creative.ambientsounds.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.world.level.block.state.BlockState;
import team.creative.ambientsounds.AmbientSounds;
import team.creative.ambientsounds.engine.AmbientEngineLoadException;

public class AmbientBlockGroup {
    
    private final List<String> data = new ArrayList<>();
    private List<AmbientBlock> filters;
    
    public void onClientLoad() {
        filters = new ArrayList<>();
        for (String condition : data)
            try {
                filters.add(AmbientBlock.parse(condition));
            } catch (AmbientEngineLoadException e) {
                AmbientSounds.LOGGER.error("Failed to load block entry {}", condition, e);
            }
    }
    
    public void add(String[] data) {
        this.data.addAll(Arrays.asList(data));
    }
    
    public boolean isEmpty() {
        return filters.isEmpty();
    }
    
    public boolean is(BlockState state) {
        for (AmbientBlock block : filters)
            if (block.is(state))
                return true;
        return false;
    }
    
}
