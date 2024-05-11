package team.creative.ambientsounds.engine;

import com.google.gson.annotations.SerializedName;

public class AmbientEngineConfig {
    
    @SerializedName("default-engine")
    public String defaultEngine = "basic";
    
    public String[] engines;
    
}
