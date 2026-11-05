package com.orbitconvert.app;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import android.app.AlertDialog;
import android.os.Looper;
import android.net.Uri;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.time.Duration;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class UiTest {
 @Test public void launchesWithoutAccountOrPermissionScreen(){try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){assertNotNull(c.get().findViewById(android.R.id.content));assertFalse(c.get().isFinishing());}}
 @Test @SuppressWarnings("unchecked") public void heldVolumeOpensAndCanReopenWheel()throws Exception{
  try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=c.get();Field fs=MainActivity.class.getDeclaredField("files"),ns=MainActivity.class.getDeclaredField("names");fs.setAccessible(true);ns.setAccessible(true);
   ((ArrayList<Uri>)fs.get(a)).add(Uri.parse("content://test/example.pdf"));((ArrayList<String>)ns.get(a)).add("example.pdf");
   a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP));Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(460));
   AlertDialog first=ShadowAlertDialog.getLatestAlertDialog();assertTrue(first.isShowing());first.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_VOLUME_UP));first.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
   a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN));Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(460));
   AlertDialog second=ShadowAlertDialog.getLatestAlertDialog();assertTrue(second.isShowing());assertNotSame(first,second);second.dismiss();
  }
 }
}
