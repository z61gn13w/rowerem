package travel;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Root {

    @SerializedName("countries")
    public List<Country> countries;

}
