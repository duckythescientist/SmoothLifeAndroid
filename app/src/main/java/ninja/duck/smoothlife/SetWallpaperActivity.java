package ninja.duck.smoothlife;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;

public class SetWallpaperActivity extends Activity {

    private static final String TAG = "SetWallpaper";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_set_wallpaper);
        Log.d(TAG, "Creating");
        doSetWallpaper();
    }

    public void setDefaultWallpaper(View view) {
        doSetWallpaper();
    }

    static final int RETURNING_FROM_WALLPAPER = 1;

    public void doSetWallpaper() {
        Log.d(TAG, "Trying to set wallpaper");
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, SLWallpaperService.class));
        startActivityForResult(intent, RETURNING_FROM_WALLPAPER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RETURNING_FROM_WALLPAPER) {
            Log.d(TAG, "Finished wallpapering");
            doSetWallpaper();
        }
    }

}
