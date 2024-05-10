package team.creative.ambientsounds.sound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import team.creative.ambientsounds.condition.AmbientCondition;
import team.creative.ambientsounds.condition.AmbientSelection;
import team.creative.ambientsounds.engine.AmbientEngine;
import team.creative.ambientsounds.engine.AmbientEngineLoadException;
import team.creative.ambientsounds.engine.AmbientStackType;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.creativecore.client.render.text.DebugTextRenderer;
import team.creative.creativecore.common.config.api.CreativeConfig;

public class AmbientSoundCategory extends AmbientCondition {
    
    public String parent;
    public AmbientStackType stack = AmbientStackType.overwrite;
    
    public transient String name;
    
    @CreativeConfig.DecimalRange(min = 0, max = 1)
    public transient double volumeSetting = 1;
    
    public transient AmbientSoundCategory parentCategory;
    public transient List<AmbientSoundCategory> children = new ArrayList<>();
    
    public transient AmbientSelection selection;
    
    @Override
    public void init(AmbientEngine engine) throws AmbientEngineLoadException {
        super.init(engine);
        parentCategory = engine.getSoundCategory(parent);
        if (parentCategory != null)
            parentCategory.children.add(this);
        if (parentCategory == this)
            throw new AmbientEngineLoadException("Sound cateogry cannot have itself as a parent " + name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    public void postInit(HashSet<String> remaining) {
        remaining.remove(name);
        for (AmbientSoundCategory cat : children)
            cat.postInit(remaining);
    }
    
    public void tick(AmbientEnvironment env, AmbientSelection parentSelection) {
        if (volumeSetting == 0)
            return;
        
        selection = value(env);
        
        if (selection != null) {
            if (parentSelection != null)
                selection.last().subSelection = parentSelection;
            
            selection.mulSetting(volumeSetting);
        }
        
        for (AmbientSoundCategory child : children)
            child.tick(env, selection);
    }
    
    public void collectDetails(DebugTextRenderer text) {
        if (selection == null)
            return;
        text.detail(name, selection.volume());
        for (AmbientSoundCategory child : children)
            child.collectDetails(text);
    }
    
}
