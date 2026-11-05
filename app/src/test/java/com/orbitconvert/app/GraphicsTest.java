package com.orbitconvert.app;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.graphics.*;
import android.view.View;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28, qualifiers="w360dp-h733dp-xxhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GraphicsTest {
 @Test public void imageConversionProducesRealJpeg()throws Exception{
  File work=Files.createTempDirectory("orbit-image").toFile();try{File in=new File(work,"image.png"),out=new File(work,"out");out.mkdir();Bitmap b=Bitmap.createBitmap(800,600,Bitmap.Config.ARGB_8888);b.eraseColor(Color.RED);try(OutputStream s=new FileOutputStream(in)){b.compress(Bitmap.CompressFormat.PNG,100,s);}b.recycle();
   ConvertEngine.Options o=new ConvertEngine.Options();o.width=400;List<File> results=new ConvertEngine(RuntimeEnvironment.getApplication(),out,s->{}).run("JPG",Collections.singletonList(in),o);
   byte[] bytes=Files.readAllBytes(results.get(0).toPath());assertEquals(0xff,bytes[0]&255);assertEquals(0xd8,bytes[1]&255);Bitmap result=BitmapFactory.decodeFile(results.get(0).getPath());assertEquals(400,result.getWidth());assertEquals(300,result.getHeight());result.recycle();
  }finally{MainActivity.delete(work);}
 }
 @Test public void renderWheelForVisualInspection()throws Exception{
  RadialMenu ring=new RadialMenu(RuntimeEnvironment.getApplication(),Formats.actions(Collections.singletonList("example.pdf")),a->{});ring.layout(0,0,900,900);Bitmap b=Bitmap.createBitmap(900,900,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.rgb(244,246,242));ring.draw(c);File folder=new File("build/screenshots");folder.mkdirs();try(OutputStream out=new FileOutputStream(new File(folder,"wheel.png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();
 }
 @Test public void renderHomeForVisualInspection()throws Exception{
  try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
   View view=c.get().findViewById(android.R.id.content);view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2200,View.MeasureSpec.EXACTLY));view.layout(0,0,1080,2200);Bitmap bitmap=Bitmap.createBitmap(1080,2200,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));File folder=new File("build/screenshots");folder.mkdirs();try(OutputStream out=new FileOutputStream(new File(folder,"home.png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
  }
 }
}
