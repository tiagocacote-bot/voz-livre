package br.pessoal.vozlivre;

import android.app.Application;
import android.graphics.*;
import android.view.View;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, application=Application.class, qualifiers="pt-rBR-w411dp-h891dp-port-mdpi", shadows=RuntimeTest.PosixAtomicRename.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RenderTest {
 @Test public void render() throws Exception {
  Repository repo=new Repository(RuntimeEnvironment.getApplication());repo.save(new Model(repo.defaults()));
  ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=c.get();
  draw(a,"inicio",411,891);a.navigate("comida");draw(a,"comida",411,891);a.navigate("nucleo");draw(a,"nucleo",411,891);c.pause().stop().destroy();
 }
 @Test @Config(qualifiers="pt-rBR-w891dp-h411dp-land-mdpi") public void landscape()throws Exception {
  Repository repo=new Repository(RuntimeEnvironment.getApplication());repo.save(new Model(repo.defaults()));
  ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=c.get();draw(a,"paisagem",891,411);
  Assert.assertTrue("Landscape must leave a full card visible",a.pageScroll.getHeight()>=a.grid.getChildAt(0).getHeight()+12);c.pause().stop().destroy();
 }
 static void draw(MainActivity a,String name,int width,int height)throws Exception {
  a.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));a.root.layout(0,0,width,height);
  Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);a.root.draw(new Canvas(b));
  try(FileOutputStream out=new FileOutputStream(new File(System.getProperty("render.dir"),name+".png"))){Assert.assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}
 }
}
