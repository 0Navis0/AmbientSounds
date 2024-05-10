package team.creative.ambientsounds.condition;

import java.util.List;

import team.creative.ambientsounds.sound.AmbientSoundProperties;

public class AmbientSelectionMulti extends AmbientSelection {
    
    protected final List<AmbientSelection> selections;
    
    public AmbientSelectionMulti(AmbientSelection selection, List<AmbientSelection> selections) {
        super(selection.condition);
        assign(selection);
        this.selections = selections;
    }
    
    @Override
    public double conditionVolume() {
        double volume = 1;
        for (AmbientSelection sel : selections)
            volume *= sel.conditionVolume();
        return volume * super.conditionVolume();
    }
    
    @Override
    public double settingVolume() {
        double volume = 1;
        for (AmbientSelection sel : selections)
            volume *= sel.settingVolume();
        return volume * super.settingVolume();
    }
    
    @Override
    public double volume() {
        double volume = 1;
        for (AmbientSelection sel : selections)
            volume *= sel.volume();
        return volume * super.volume();
    }
    
    @Override
    protected void assignProperties(AmbientSoundProperties properties) {
        for (AmbientSelection sel : selections)
            sel.assignProperties(properties);
        
        super.assignProperties(properties);
    }
    
}
