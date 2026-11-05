package com.orbitconvert.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int PICK=10,TREE=11;
    private final ArrayList<Uri> files=new ArrayList<>();
    private final ArrayList<String> names=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout root,fileList,recent;private TextView status,folderLabel;private ProgressBar progress;
    private Button add,folder,convert,clear,cancel;private EditText width,quality,pages;private Spinner ratio;
    private Uri outputTree;private volatile boolean busy=false;private Future<?> job;private AlertDialog wheel;
    private boolean volumeHeld=false,volumeOpened=false;private int heldCode;
    private final Runnable volumeTrigger=()->{if(volumeHeld&&!busy){volumeOpened=true;showWheel();}};
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);buildUi();String savedTree=getPreferences(0).getString("tree","");if(!savedTree.isEmpty())outputTree=Uri.parse(savedTree);updateFolder();
        if(saved!=null){ArrayList<String> uris=saved.getStringArrayList("files");if(uris!=null)for(String s:uris)addFile(Uri.parse(s));}else acceptShare(getIntent());
        drawFiles();drawRecent();worker.submit(()->{File[] old=getCacheDir().listFiles();if(old!=null)for(File f:old)if(f.getName().startsWith("job-"))delete(f);});
    }
    private TextView label(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(7),0,dp(7));return v;}
    private Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(244,246,242));root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(18),dp(22),dp(30));scroll.addView(root);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        TextView tag=label("O R B I T   /   O F F L I N E",12,Color.rgb(36,92,80));tag.setTypeface(null,Typeface.BOLD);root.addView(tag);
        TextView title=label("Your files.\nA new form.",36,Color.rgb(23,61,53));title.setTypeface(null,Typeface.BOLD);root.addView(title);
        root.addView(label("Convert on your phone. No account. No uploads.",15,Color.DKGRAY));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(222,235,214));bg.setCornerRadius(dp(20));card.setBackground(bg);
        card.addView(label("01  •  Choose your files",18,Color.rgb(23,61,53)));
        add=button("＋  Select files",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);startActivityForResult(i,PICK);});card.addView(add);
        fileList=new LinearLayout(this);fileList.setOrientation(LinearLayout.VERTICAL);card.addView(fileList);
        clear=button("Clear selection",()->{files.clear();names.clear();drawFiles();});card.addView(clear);root.addView(card);
        root.addView(label("02  •  Save on this device",18,Color.rgb(23,61,53)));folder=button("Choose output folder",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);startActivityForResult(i,TREE);});root.addView(folder);folderLabel=label("",12,Color.DKGRAY);root.addView(folderLabel);
        root.addView(label("03  •  Choose a conversion",18,Color.rgb(23,61,53)));root.addView(label("Select files, then hold Volume Up or Down for half a second. Or tap the button below. The shortcut works while this screen is open.",14,Color.DKGRAY));
        convert=button("Open conversion wheel  ◉",this::showWheel);root.addView(convert);
        LinearLayout settings=new LinearLayout(this);settings.setOrientation(LinearLayout.VERTICAL);
        width=field(settings,"Maximum image / PDF render width (100–4096 px)","1600",true);quality=field(settings,"JPG / WEBP quality (10–100)","90",true);pages=field(settings,"PDF pages — blank for all; example 1-3, 5, 8-","",false);
        settings.addView(label("Center crop ratio (Crop action only)",12,Color.DKGRAY));ratio=new Spinner(this);ratio.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"1:1 square","4:3 landscape","16:9 landscape","3:4 portrait"}));settings.addView(ratio);
        Button options=button("Output settings ▾",()->settings.setVisibility(settings.getVisibility()==View.GONE?View.VISIBLE:View.GONE));root.addView(options);settings.setVisibility(View.GONE);root.addView(settings);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setVisibility(View.GONE);root.addView(progress);status=label("Ready. Original files are never overwritten.",14,Color.rgb(36,92,80));root.addView(status);
        cancel=button("Cancel conversion",()->{if(job!=null)job.cancel(true);status.setText("Cancelling…");cancel.setEnabled(false);});cancel.setVisibility(View.GONE);root.addView(cancel);
        root.addView(label("Recent results",20,Color.rgb(23,61,53)));recent=new LinearLayout(this);recent.setOrientation(LinearLayout.VERTICAL);root.addView(recent);
        root.addView(button("Clear recent list",()->{getPreferences(0).edit().remove("history").apply();drawRecent();}));root.addView(button("Supported features & privacy",this::about));
    }
    private EditText field(LinearLayout parent,String caption,String value,boolean number){parent.addView(label(caption,12,Color.DKGRAY));EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setContentDescription(caption);if(number)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);parent.addView(e);return e;}
    private void addFile(Uri uri){if(uri==null||!"content".equals(uri.getScheme())||files.contains(uri)||files.size()>=50)return;String n="file_"+(files.size()+1);try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())n=c.getString(0);}catch(Exception ignored){}files.add(uri);names.add(n==null?"file":n);}
    private void acceptShare(Intent intent){String action=intent.getAction();if(Intent.ACTION_SEND.equals(action))addFile(intent.getParcelableExtra(Intent.EXTRA_STREAM));else if(Intent.ACTION_SEND_MULTIPLE.equals(action)){ArrayList<Uri> uris=intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(uris!=null)for(Uri uri:uris)addFile(uri);}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        try{if(request==TREE){Uri uri=data.getData();if(uri==null)return;if(!"com.android.externalstorage.documents".equals(uri.getAuthority()))throw new IOException("Choose Internal storage or an SD card. Cloud output folders are not supported.");if((data.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0)throw new IOException("The selected folder does not grant write access");getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);outputTree=uri;getPreferences(0).edit().putString("tree",uri.toString()).apply();updateFolder();}
            else if(request==PICK){ArrayList<Uri> picked=new ArrayList<>();if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)picked.add(data.getClipData().getItemAt(i).getUri());else if(data.getData()!=null)picked.add(data.getData());for(Uri uri:picked){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}addFile(uri);}drawFiles();}
        }catch(Exception e){error(e);}
    }
    private void updateFolder(){folderLabel.setText(outputTree==null?"Choose a local folder and tap “Use this folder”. OrbitConvert creates a separate folder for each job.":"Selected: "+DocumentsContract.getTreeDocumentId(outputTree)+"\nResults go into a new OrbitConvert job folder.");}
    private void drawFiles(){fileList.removeAllViews();if(files.isEmpty())fileList.addView(label("PDFs, images, documents and more.\nYou can also Share files from another app.",14,Color.DKGRAY));
        for(int i=0;i<names.size();i++){final int index=i;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView text=label((i+1)+". "+names.get(i),14,Color.rgb(23,61,53));row.addView(text,new LinearLayout.LayoutParams(0,-2,1));Button up=button("↑",()->{if(index>0){Collections.swap(files,index,index-1);Collections.swap(names,index,index-1);drawFiles();}});up.setContentDescription("Move "+names.get(i)+" earlier");up.setEnabled(!busy&&i>0);row.addView(up,new LinearLayout.LayoutParams(dp(48),dp(48)));Button remove=button("×",()->{files.remove(index);names.remove(index);drawFiles();});remove.setContentDescription("Remove "+names.get(i));remove.setEnabled(!busy);row.addView(remove,new LinearLayout.LayoutParams(dp(48),dp(48)));fileList.addView(row);}
        convert.setEnabled(!busy&&!files.isEmpty());clear.setEnabled(!busy&&!files.isEmpty());
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){int code=e.getKeyCode();if((code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_VOLUME_DOWN)&&!files.isEmpty()&&!busy){
        if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getRepeatCount()==0&&!volumeHeld){volumeHeld=true;volumeOpened=false;heldCode=code;handler.postDelayed(volumeTrigger,450);}return true;}
        if(e.getAction()==KeyEvent.ACTION_UP){handler.removeCallbacks(volumeTrigger);volumeHeld=false;if(!volumeOpened)((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,heldCode==KeyEvent.KEYCODE_VOLUME_UP?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);return true;}}
        return super.dispatchKeyEvent(e);
    }
    private void showWheel(){if(busy||files.isEmpty()||(wheel!=null&&wheel.isShowing()))return;List<String> actions=Formats.actions(names);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);RadialMenu ring=new RadialMenu(this,actions,this::choose);box.addView(ring,new LinearLayout.LayoutParams(-1,Math.min(dp(300),getResources().getDisplayMetrics().heightPixels/2)));
        TextView help=label("Tap a slice • Tap center for more\nOr choose from the accessible list:",12,Color.DKGRAY);help.setPadding(dp(18),0,dp(18),0);box.addView(help);
        ScrollView list=new ScrollView(this);LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.VERTICAL);for(String action:actions)buttons.addView(button(action,()->choose(action)));list.addView(buttons);box.addView(list,new LinearLayout.LayoutParams(-1,Math.min(dp(170),getResources().getDisplayMetrics().heightPixels/4)));
        wheel=new AlertDialog.Builder(this).setTitle("Convert "+files.size()+" file(s)").setView(box).setNegativeButton("Close",null).create();wheel.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_VOLUME_UP||key==KeyEvent.KEYCODE_VOLUME_DOWN){if(event.getAction()==KeyEvent.ACTION_UP){volumeHeld=false;handler.removeCallbacks(volumeTrigger);}return true;}return false;});wheel.setOnDismissListener(d->{volumeHeld=false;handler.removeCallbacks(volumeTrigger);});wheel.show();
    }
    private void choose(String action){if(wheel!=null)wheel.dismiss();if(outputTree==null){status.setText("Choose an output folder first, then open the wheel again.");folder.performClick();return;}
        if(action.equals("Rasterize PDF")){new AlertDialog.Builder(this).setTitle("Rasterize PDF?").setMessage("Pages become images. Searchable text, links, forms and vector quality are lost. This may not reduce file size. Originals stay unchanged.").setPositiveButton("Continue",(d,w)->start(action)).setNegativeButton("Cancel",null).show();}else start(action);
    }
    private void start(String action){if(busy)return;ConvertEngine.Options options=new ConvertEngine.Options();try{options.width=Integer.parseInt(width.getText().toString());options.quality=Integer.parseInt(quality.getText().toString());if(options.width<100||options.width>4096||options.quality<10||options.quality>100)throw new IllegalArgumentException("Width: 100–4096; quality: 10–100");options.pages=pages.getText().toString();options.cropRatio=new float[]{1,4f/3,16f/9,3f/4}[ratio.getSelectedItemPosition()];}catch(Exception e){error(e);return;}
        ArrayList<Uri> sources=new ArrayList<>(files);ArrayList<String> sourceNames=new ArrayList<>(names);Uri tree=outputTree;setBusy(true);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);status.setText("Preparing local files…");
        job=worker.submit(()->{File work=new File(getCacheDir(),"job-"+UUID.randomUUID());File input=new File(work,"input"),output=new File(work,"output");Uri jobFolder=null;
            try{if(!input.mkdirs()||!output.mkdirs())throw new IOException("Could not create temporary folder");List<File> local=new ArrayList<>();long bytes=0;
                for(int i=0;i<sources.size();i++){String safe=sourceNames.get(i).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_");if(safe.length()>150)safe=safe.substring(safe.length()-150);File f=new File(input,String.format(Locale.ROOT,"%02d_",i+1)+safe);try(InputStream in=getContentResolver().openInputStream(sources.get(i));OutputStream out=new FileOutputStream(f)){if(in==null)throw new IOException("File is unavailable");bytes+=ConvertEngine.copy(in,out,Math.min(256L*1024*1024,512L*1024*1024-bytes));}local.add(f);}
                List<File> results=new ConvertEngine(getApplicationContext(),output,msg->runOnUiThread(()->status.setText(msg))).run(action,local,options);
                if(Thread.currentThread().isInterrupted())throw new IOException("Cancelled");Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
                jobFolder=DocumentsContract.createDocument(getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,"OrbitConvert_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",Locale.ROOT).format(new Date()));if(jobFolder==null)throw new IOException("Cannot create output folder. Choose the folder again.");
                JSONArray saved=new JSONArray();for(File f:results){if(Thread.currentThread().isInterrupted())throw new IOException("Cancelled");Uri dest=DocumentsContract.createDocument(getContentResolver(),jobFolder,mime(f.getName()),f.getName());if(dest==null)throw new IOException("Cannot create output file");try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(dest,"w")){if(out==null)throw new IOException("Cannot write output file");ConvertEngine.copy(in,out,Long.MAX_VALUE);}JSONObject item=new JSONObject();item.put("name",f.getName());item.put("uri",dest.toString());saved.put(item);}
                if(Thread.currentThread().isInterrupted())throw new IOException("Cancelled");
                runOnUiThread(()->{remember(saved);status.setText("Saved "+saved.length()+" result(s) in your selected folder.");drawRecent();});
            }catch(Exception|OutOfMemoryError e){if(jobFolder!=null){try{DocumentsContract.deleteDocument(getContentResolver(),jobFolder);}catch(Exception ignored){runOnUiThread(()->Toast.makeText(this,"A partial job folder could not be removed. Check your output folder.",Toast.LENGTH_LONG).show());}}
                String msg=Thread.currentThread().isInterrupted()?"Conversion cancelled. Originals are unchanged.":e instanceof OutOfMemoryError?"Not enough memory. Try fewer pages or a smaller width.":e.getMessage();runOnUiThread(()->status.setText("Stopped: "+msg));
            }finally{delete(work);runOnUiThread(()->{setBusy(false);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);});}
        });
    }
    private void setBusy(boolean value){busy=value;add.setEnabled(!value);folder.setEnabled(!value);width.setEnabled(!value);quality.setEnabled(!value);pages.setEnabled(!value);ratio.setEnabled(!value);progress.setVisibility(value?View.VISIBLE:View.GONE);cancel.setVisibility(value?View.VISIBLE:View.GONE);cancel.setEnabled(value);drawFiles();}
    private void remember(JSONArray latest){try{JSONArray all=new JSONArray();for(int i=0;i<latest.length()&&all.length()<30;i++)all.put(latest.get(i));JSONArray old=new JSONArray(getPreferences(0).getString("history","[]"));for(int i=0;i<old.length()&&all.length()<30;i++)all.put(old.get(i));getPreferences(0).edit().putString("history",all.toString()).apply();}catch(JSONException ignored){}}
    private void drawRecent(){recent.removeAllViews();try{JSONArray all=new JSONArray(getPreferences(0).getString("history","[]"));if(all.length()==0)recent.addView(label("Your converted files will appear here.",14,Color.GRAY));for(int i=0;i<all.length();i++){JSONObject item=all.getJSONObject(i);String name=item.getString("name");Uri uri=Uri.parse(item.getString("uri"));recent.addView(button(name,()->new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Open","Share"},(d,which)->open(uri,name,which==1)).show()));}}catch(Exception ignored){}}
    private void open(Uri uri,String name,boolean share){try{Intent i=new Intent(share?Intent.ACTION_SEND:Intent.ACTION_VIEW);if(share){i.setType(mime(name));i.putExtra(Intent.EXTRA_STREAM,uri);}else i.setDataAndType(uri,mime(name));i.setClipData(ClipData.newRawUri(name,uri));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,share?"Share result":"Open result"));}catch(Exception e){error(e);}}
    static String mime(String name){String ext=Formats.ext(name);String m=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);return m==null?"application/octet-stream":m;}
    private void about(){new AlertDialog.Builder(this).setTitle("Orbit Convert 0.1.0 • Pilot").setMessage("Local image, PDF, text, ZIP, table and subtitle tools. Basic AAC extraction and video snapshots. Not a complete Filee port.\n\nImages: JPEG, PNG, WEBP, BMP and first GIF frame. HEIC input depends on Android.\n\nNot included: full Office layout conversion, OCR, video/audio transcoding, CAD, fonts, 3D, advanced e-books, background folder watching.\n\nNo INTERNET permission, login, analytics or cloud output. Select local input files; another app/provider may itself download a shared cloud file. Temporary copies are deleted after each job. Only output names/URIs remain in local recent history.\n\nKeep the app open during conversion. Maximum 50 input files, 256 MB each / 512 MB total. ZIP extraction flattens folders. Center crop is fixed-ratio. Rasterized PDF loses searchable text.\n\nVolume shortcut works in this app, not inside another file manager. Use Android Share to bring files here.\n\nPage-range logic adapted from Filee, © 2026 Myeonggu Kim and contributors (MIT). PDFBox Android is Apache-2.0. See bundled licenses.").setPositiveButton("OK",null).setNeutralButton("Licenses",(d,w)->licenses()).show();}
    private void licenses(){try{StringBuilder text=new StringBuilder();for(String name:new String[]{"FILEE-LICENSE.txt","THIRD-PARTY-NOTICES.txt","APACHE-2.0.txt","PDFBOX-NOTICE.txt","BOUNCYCASTLE-LICENSE.txt"})try(InputStream s=getAssets().open(name)){ByteArrayOutputStream b=new ByteArrayOutputStream();ConvertEngine.copy(s,b,100000);text.append(b.toString("UTF-8")).append("\n\n");}new AlertDialog.Builder(this).setTitle("Open-source notices").setMessage(text).setPositiveButton("OK",null).show();}catch(Exception e){error(e);}}
    private void error(Throwable e){new AlertDialog.Builder(this).setTitle("Unable to continue").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
    static void delete(File f){if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File c:children)delete(c);}f.delete();}
    @Override protected void onSaveInstanceState(Bundle out){ArrayList<String> list=new ArrayList<>();for(Uri u:files)list.add(u.toString());out.putStringArrayList("files",list);super.onSaveInstanceState(out);}
    @Override protected void onPause(){handler.removeCallbacks(volumeTrigger);volumeHeld=false;super.onPause();}
    @Override public void onBackPressed(){if(busy){new AlertDialog.Builder(this).setMessage("Cancel conversion and leave?").setPositiveButton("Cancel job",(d,w)->{if(job!=null)job.cancel(true);finish();}).setNegativeButton("Stay",null).show();}else super.onBackPressed();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(job!=null)job.cancel(true);worker.shutdownNow();super.onDestroy();}
}
