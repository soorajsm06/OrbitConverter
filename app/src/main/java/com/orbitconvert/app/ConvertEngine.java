package com.orbitconvert.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.media.*;
import android.os.ParcelFileDescriptor;
import android.text.*;
import androidx.exifinterface.media.ExifInterface;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import org.json.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Offline engines. Every result is staged privately until the complete job succeeds. */
public final class ConvertEngine {
    private static final class ClosingPdfDocument extends PdfDocument implements AutoCloseable {}
    public interface Progress { void report(String message); }
    public static final class Options {
        public int quality=90, width=1600;
        public String pages="";
        public float cropRatio=1f;
    }
    private final File out;
    private final Progress progress;
    public ConvertEngine(Context c,File output,Progress p) { out=output; progress=p; PDFBoxResourceLoader.init(c.getApplicationContext()); }
    private void check() throws IOException { if(Thread.currentThread().isInterrupted()) throw new IOException("Cancelled"); }
    private File target(String name) throws IOException {
        String safe=name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_");
        if(safe.length()>160) { int dot=safe.lastIndexOf('.'); safe=safe.substring(0,130)+(dot>=130 && safe.length()-dot<20?safe.substring(dot):""); }
        File f=new File(out,safe); int i=2;
        while(f.exists()) { int dot=safe.lastIndexOf('.'); f=new File(out,(dot<0?safe:safe.substring(0,dot))+"_"+(i++)+(dot<0?"":safe.substring(dot))); }
        if(!f.createNewFile()) throw new IOException("Could not create output"); return f;
    }
    private static String base(File f) { String n=f.getName(); int i=n.lastIndexOf('.'); return i<0?n:n.substring(0,i); }
    public List<File> run(String action,List<File> inputs,Options o) throws Exception {
        if(inputs.isEmpty()) throw new IOException("Select files first");
        if(o.width<100||o.width>4096||o.quality<10||o.quality>100) throw new IOException("Width must be 100–4096; quality 10–100");
        if(action.equals("Create ZIP")) { zip(inputs); }
        else if(action.equals("Extract ZIP")) { unzip(inputs.get(0)); }
        else if(action.equals("Images to PDF")) { imagesPdf(inputs,o); }
        else if(action.equals("Merge PDF")) { merge(inputs,o); }
        else for(File f:inputs) {
            check(); progress.report("Converting "+f.getName());
            switch(action) {
                case "PNG": case "JPG": case "WEBP": case "Resize": case "Grayscale": case "Crop": image(f,action,o); break;
                case "PDF to PNG": case "PDF to JPG": case "PDF to long image": case "Rasterize PDF": renderPdf(f,action,o); break;
                case "PDF to TXT": case "Extract pages": case "Split PDF": pdfTools(f,action,o); break;
                case "Text to PDF": textPdf(readText(f),base(f)); break;
                case "Text to DOCX": docx(readText(f),base(f)); break;
                case "Text to HTML": write(target(base(f)+".html"),"<!doctype html><meta charset=\"utf-8\"><pre>"+xml(readText(f))+"</pre>"); break;
                case "DOCX to TXT": docxText(f); break;
                case "CSV": case "TSV": case "Table to JSON": table(f,action); break;
                case "JSON to CSV": jsonCsv(f); break;
                case "Format JSON": Object value=new JSONTokener(readText(f)).nextValue(); write(target(base(f)+".json"),value instanceof JSONObject?((JSONObject)value).toString(2):((JSONArray)value).toString(2)); break;
                case "SRT to VTT": write(target(base(f)+".vtt"),"WEBVTT\n\n"+readText(f).replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})","$1.$2")); break;
                case "VTT to SRT": vtt(f); break;
                case "Extract AAC": extractAudio(f); break;
                case "Video frame": frame(f,o); break;
                default: throw new IOException("Conversion is not implemented: "+action);
            }
        }
        File[] files=out.listFiles(); if(files==null||files.length==0) throw new IOException("No output was produced");
        return Arrays.asList(files);
    }
    private Bitmap load(File f,int width) throws IOException {
        BitmapFactory.Options opt=new BitmapFactory.Options(); opt.inJustDecodeBounds=true; BitmapFactory.decodeFile(f.getPath(),opt);
        if(opt.outWidth<1||opt.outHeight<1) throw new IOException("Image format is not decoded on this device: "+f.getName());
        opt.inSampleSize=1;
        while((long)(opt.outWidth/opt.inSampleSize)*(opt.outHeight/opt.inSampleSize)>8_000_000L || opt.outWidth/opt.inSampleSize>width*2) opt.inSampleSize*=2;
        opt.inJustDecodeBounds=false; Bitmap b=BitmapFactory.decodeFile(f.getPath(),opt);
        if(b==null) throw new IOException("Could not decode image");
        try {
            ExifInterface exif=new ExifInterface(f); Matrix m=new Matrix();
            int orientation=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            switch(orientation) {
                case 2:m.setScale(-1,1);break; case 3:m.setRotate(180);break; case 4:m.setScale(1,-1);break;
                case 5:m.setRotate(90);m.postScale(-1,1);break; case 6:m.setRotate(90);break;
                case 7:m.setRotate(-90);m.postScale(-1,1);break; case 8:m.setRotate(-90);break;
            }
            if(!m.isIdentity()) { Bitmap transformed=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true); if(transformed!=b)b.recycle(); b=transformed; }
        } catch(IOException ignored) { /* Some formats have no EXIF. */ }
        if(b.getWidth()>width) { Bitmap scaled=Bitmap.createScaledBitmap(b,width,Math.max(1,(int)((long)b.getHeight()*width/b.getWidth())),true); if(scaled!=b)b.recycle(); b=scaled; }
        return b;
    }
    private void image(File f,String action,Options o) throws Exception {
        Bitmap b=load(f,o.width);
        try {
            if(action.equals("Crop")) {
                int w=b.getWidth(),h=b.getHeight();
                if((float)w/h>o.cropRatio) w=Math.max(1,(int)(h*o.cropRatio)); else h=Math.max(1,(int)(w/o.cropRatio));
                Bitmap c=Bitmap.createBitmap(b,(b.getWidth()-w)/2,(b.getHeight()-h)/2,w,h); if(c!=b)b.recycle(); b=c;
            }
            if(action.equals("Grayscale")) {
                Bitmap g=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);
                Paint p=new Paint(); ColorMatrix m=new ColorMatrix();m.setSaturation(0);p.setColorFilter(new ColorMatrixColorFilter(m));new Canvas(g).drawBitmap(b,0,0,p);b.recycle();b=g;
            }
            String format=action.equals("JPG")?"jpg":action.equals("WEBP")?"webp":"png";
            saveBitmap(b,target(base(f)+"_converted."+format),format,o.quality);
        } finally { b.recycle(); }
    }
    private static void saveBitmap(Bitmap b,File f,String format,int quality) throws IOException {
        Bitmap flat=b;
        if(format.equals("jpg")) { flat=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(flat);c.drawColor(Color.WHITE);c.drawBitmap(b,0,0,null); }
        try(OutputStream s=new FileOutputStream(f)) {
            Bitmap.CompressFormat cf=format.equals("jpg")?Bitmap.CompressFormat.JPEG:format.equals("webp")?Bitmap.CompressFormat.WEBP:Bitmap.CompressFormat.PNG;
            if(!flat.compress(cf,quality,s)) throw new IOException("Image encoder failed");
        } finally { if(flat!=b)flat.recycle(); }
    }
    private void imagesPdf(List<File> files,Options o) throws Exception {
        try(ClosingPdfDocument doc=new ClosingPdfDocument()) {
            int number=1;for(File f:files) { check();Bitmap b=load(f,o.width); try { addPage(doc,b,number++); }finally {b.recycle();} }
            try(OutputStream s=new FileOutputStream(target("images.pdf"))) {doc.writeTo(s);}
        }
    }
    private static void addPage(PdfDocument doc,Bitmap b,int number) {
        int w=595,h=Math.max(1,Math.round(595f*b.getHeight()/b.getWidth()));
        PdfDocument.Page p=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,number).create());p.getCanvas().drawColor(Color.WHITE);
        p.getCanvas().drawBitmap(b,null,new Rect(0,0,w,h),new Paint(Paint.FILTER_BITMAP_FLAG));doc.finishPage(p);
    }
    private Bitmap render(PdfRenderer renderer,int index,int width) throws IOException {
        check();try(PdfRenderer.Page p=renderer.openPage(index)) {
            int h=Math.max(1,Math.round(width*(float)p.getHeight()/p.getWidth()));
            if((long)width*h>12_000_000) throw new IOException("Page too tall; reduce output width");
            Bitmap b=Bitmap.createBitmap(width,h,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);return b;
        }
    }
    private void renderPdf(File f,String action,Options o) throws Exception {
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer r=new PdfRenderer(fd)) {
            List<Integer> pages=PageRange.parse(o.pages,r.getPageCount());
            if(action.equals("PDF to long image")) {
                long height=0; for(int i:pages) {try(PdfRenderer.Page p=r.openPage(i)){height+=Math.max(1,Math.round(o.width*(float)p.getHeight()/p.getWidth()));}}
                if(height>30000||height*o.width>20_000_000) throw new IOException("Combined image too large. Reduce width or select fewer pages (20 megapixel limit).");
                Bitmap joined=Bitmap.createBitmap(o.width,(int)height,Bitmap.Config.ARGB_8888);
                try { Canvas c=new Canvas(joined); int y=0;for(int i:pages){Bitmap b=render(r,i,o.width);c.drawBitmap(b,0,y,null);y+=b.getHeight();b.recycle();}saveBitmap(joined,target(base(f)+"_long.png"),"png",100); }finally{joined.recycle();}
            } else if(action.equals("Rasterize PDF")) {
                try(ClosingPdfDocument doc=new ClosingPdfDocument()) {
                    int n=1;for(int i:pages){Bitmap b=render(r,i,o.width);try{addPage(doc,b,n++);}finally{b.recycle();}}
                    try(OutputStream s=new FileOutputStream(target(base(f)+"_rasterized.pdf"))){doc.writeTo(s);}
                }
            } else {String type=action.equals("PDF to JPG")?"jpg":"png";for(int i:pages){progress.report("Rendering page "+(i+1));Bitmap b=render(r,i,o.width);try{saveBitmap(b,target(base(f)+String.format(Locale.ROOT,"_page_%04d.",i+1)+type),type,o.quality);}finally{b.recycle();}}}
        }
    }
    private PDDocument pdf(File f) throws IOException {
        PDDocument d=PDDocument.load(f,MemoryUsageSetting.setupTempFileOnly());
        if(d.isEncrypted()) {d.close();throw new IOException("Password-protected PDFs are not supported in this version");}return d;
    }
    private void merge(List<File> files,Options o) throws Exception {
        try(PDDocument dest=new PDDocument()) {
            for(File f:files){check();try(PDDocument src=pdf(f)){for(int i:PageRange.parse(o.pages,src.getNumberOfPages()))dest.importPage(src.getPage(i));}}
            dest.save(target("merged.pdf"));
        }
    }
    private void pdfTools(File f,String action,Options o) throws Exception {
        try(PDDocument src=pdf(f)) {
            List<Integer> pages=PageRange.parse(o.pages,src.getNumberOfPages());
            if(action.equals("PDF to TXT")) {
                PDFTextStripper stripper=new PDFTextStripper();StringBuilder text=new StringBuilder();
                for(int i:pages){check();stripper.setStartPage(i+1);stripper.setEndPage(i+1);text.append(stripper.getText(src));}
                if(text.toString().trim().isEmpty())throw new IOException("This PDF has no extractable text. Scanned pages require OCR, which is not included.");
                write(target(base(f)+".txt"),text.toString());
            } else if(action.equals("Split PDF")) {for(int i:pages){check();try(PDDocument d=new PDDocument()){d.importPage(src.getPage(i));d.save(target(base(f)+"_page_"+(i+1)+".pdf"));}}}
            else try(PDDocument d=new PDDocument()){for(int i:pages)d.importPage(src.getPage(i));d.save(target(base(f)+"_pages.pdf"));}
        }
    }
    private void textPdf(String text,String name) throws Exception {
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(12);paint.setColor(Color.BLACK);
        StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,515).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).build();
        try(ClosingPdfDocument doc=new ClosingPdfDocument()) {
            int line=0,n=1;
            while(line<layout.getLineCount()) {check();int top=layout.getLineTop(line),end=line;
                while(end+1<layout.getLineCount()&&layout.getLineBottom(end+1)-top<=750)end++;
                PdfDocument.Page p=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,n++).create());Canvas c=p.getCanvas();c.save();c.translate(40,40);c.clipRect(0,0,515,layout.getLineBottom(end)-top);c.translate(0,-top);layout.draw(c);c.restore();doc.finishPage(p);line=end+1;
            }
            try(OutputStream s=new FileOutputStream(target(name+".pdf"))){doc.writeTo(s);}
        }
    }
    public static String readText(File f) throws IOException {if(f.length()>8_000_000)throw new IOException("Text input exceeds 8 MB limit");return new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8).replace("\uFEFF","");}
    private static void write(File f,String s) throws IOException {try(OutputStream out=new FileOutputStream(f)){out.write(s.getBytes(StandardCharsets.UTF_8));}}
    private static String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private void docx(String text,String name) throws Exception {
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(target(name+".docx")))) {
            entry(z,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            entry(z,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            StringBuilder b=new StringBuilder("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
            for(String line:text.split("\n",-1))b.append("<w:p><w:r><w:t xml:space=\"preserve\">").append(xml(line)).append("</w:t></w:r></w:p>");
            b.append("<w:sectPr/></w:body></w:document>");entry(z,"word/document.xml",b.toString());
        }
    }
    private static void entry(ZipOutputStream z,String name,String text) throws IOException {z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    private void docxText(File f) throws Exception {
        try(ZipFile z=new ZipFile(f)) {ZipEntry e=z.getEntry("word/document.xml");if(e==null)throw new IOException("Not a DOCX file");
            org.xmlpull.v1.XmlPullParser p=android.util.Xml.newPullParser();StringBuilder b=new StringBuilder();
            try(InputStream s=new BoundedInputStream(z.getInputStream(e),8_000_000)) {p.setInput(s,"UTF-8");boolean inText=false;
                for(int event=p.getEventType();event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT;event=p.next()) {
                    if(event==org.xmlpull.v1.XmlPullParser.START_TAG){if(p.getName().equals("t"))inText=true;if(p.getName().equals("tab"))b.append('\t');if(p.getName().equals("br"))b.append('\n');}
                    if(event==org.xmlpull.v1.XmlPullParser.TEXT&&inText)b.append(p.getText());
                    if(event==org.xmlpull.v1.XmlPullParser.END_TAG){if(p.getName().equals("t"))inText=false;if(p.getName().equals("p"))b.append('\n');}
                }
            }write(target(base(f)+".txt"),b.toString());
        }
    }
    private void zip(List<File> files) throws Exception {
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(target("files.zip")))) {for(File f:files){check();z.putNextEntry(new ZipEntry(f.getName()));try(InputStream s=new FileInputStream(f)){copy(s,z,512L*1024*1024);}z.closeEntry();}}
    }
    private void unzip(File f) throws Exception {
        long total=0;int count=0;
        try(ZipInputStream z=new ZipInputStream(new FileInputStream(f))) {ZipEntry e;while((e=z.getNextEntry())!=null){check();if(e.isDirectory())continue;
            if(++count>500)throw new IOException("Archive exceeds 500-file limit");
            String n=e.getName().replace('\\','/');if(n.startsWith("/")||Arrays.asList(n.split("/")).contains("..")||n.contains(":"))throw new IOException("Unsafe archive path rejected");
            File dest=target(n.replace('/','_'));try(OutputStream s=new FileOutputStream(dest)){total+=copy(z,s,Math.min(128L*1024*1024,512L*1024*1024-total));}z.closeEntry();
        }}
    }
    public static long copy(InputStream in,OutputStream out,long limit) throws IOException {
        byte[] buf=new byte[32768];long total=0;int n;while((n=in.read(buf))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("Cancelled");total+=n;if(total>limit)throw new IOException("File exceeds size limit");out.write(buf,0,n);}return total;
    }
    private static class BoundedInputStream extends FilterInputStream {
        long remain;BoundedInputStream(InputStream s,long max){super(s);remain=max;}
        public int read()throws IOException{int n=super.read();if(n>=0&&--remain<0)throw new IOException("Document too large");return n;}
        public int read(byte[] b,int o,int l)throws IOException{int n=in.read(b,o,l);if(n>0&&(remain-=n)<0)throw new IOException("Document too large");return n;}
    }
    public static List<List<String>> parseTable(String text,char sep) throws IOException {
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;
        for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='"'){if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else quoted=!quoted;}
            else if(!quoted&&(c==sep||c=='\n'||c=='\r')){row.add(cell.toString());cell.setLength(0);if(c!=sep){rows.add(row);row=new ArrayList<>();if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;}}
            else cell.append(c);
        }
        if(quoted)throw new IOException("Unclosed quoted field");if(cell.length()>0||!row.isEmpty()){row.add(cell.toString());rows.add(row);}return rows;
    }
    public static String tableText(List<List<String>> rows,char sep){StringBuilder b=new StringBuilder();for(List<String> row:rows){for(int i=0;i<row.size();i++){if(i>0)b.append(sep);String v=row.get(i);b.append('"').append(v.replace("\"","\"\"")).append('"');}b.append('\n');}return b.toString();}
    private void table(File f,String action)throws Exception {
        List<List<String>> rows=parseTable(readText(f),Formats.ext(f.getName()).equals("tsv")?'\t':',');
        if(action.equals("Table to JSON")){if(rows.isEmpty())throw new IOException("Empty table");List<String> keys=rows.get(0);if(new HashSet<>(keys).size()!=keys.size()||keys.contains(""))throw new IOException("JSON conversion requires unique, non-empty header names");JSONArray a=new JSONArray();for(int r=1;r<rows.size();r++){if(rows.get(r).size()!=keys.size())throw new IOException("Row "+(r+1)+" has a different column count");JSONObject obj=new JSONObject();for(int i=0;i<keys.size();i++)obj.put(keys.get(i),rows.get(r).get(i));a.put(obj);}write(target(base(f)+".json"),a.toString(2));}
        else write(target(base(f)+"."+action.toLowerCase(Locale.ROOT)),tableText(rows,action.equals("CSV")?',':'\t'));
    }
    private void jsonCsv(File f)throws Exception {
        JSONArray a=new JSONArray(readText(f));LinkedHashSet<String> keys=new LinkedHashSet<>();for(int i=0;i<a.length();i++){Iterator<String> k=a.getJSONObject(i).keys();while(k.hasNext())keys.add(k.next());}
        List<List<String>> rows=new ArrayList<>();rows.add(new ArrayList<>(keys));for(int i=0;i<a.length();i++){List<String> row=new ArrayList<>();for(String k:keys)row.add(a.getJSONObject(i).isNull(k)?"":a.getJSONObject(i).optString(k,""));rows.add(row);}write(target(base(f)+".csv"),tableText(rows,','));
    }
    private void vtt(File f)throws Exception {
        String[] blocks=readText(f).replace("\r","").split("\n\\s*\n");StringBuilder b=new StringBuilder();int i=1;
        for(String block:blocks){String[] lines=block.split("\n");int time=-1;for(int j=0;j<lines.length;j++)if(lines[j].contains(" --> ")){time=j;break;}if(time<0)continue;
            String line=lines[time];java.util.regex.Matcher m=java.util.regex.Pattern.compile("((?:\\d{2}:)?\\d{2}:\\d{2})\\.(\\d{3})").matcher(line);List<String> stamps=new ArrayList<>();while(m.find())stamps.add((m.group(1).length()==5?"00:":"")+m.group(1)+","+m.group(2));if(stamps.size()!=2)throw new IOException("Unsupported VTT timestamp");
            b.append(i++).append('\n').append(stamps.get(0)).append(" --> ").append(stamps.get(1)).append('\n');for(int j=time+1;j<lines.length;j++)b.append(lines[j]).append('\n');b.append('\n');
        }if(i==1)throw new IOException("No subtitle cues found");write(target(base(f)+".srt"),b.toString());
    }
    private void extractAudio(File f)throws Exception {
        MediaExtractor ex=new MediaExtractor();MediaMuxer mux=null;boolean started=false;
        try{ex.setDataSource(f.getPath());int track=-1;MediaFormat format=null;for(int i=0;i<ex.getTrackCount();i++){MediaFormat candidate=ex.getTrackFormat(i);if("audio/mp4a-latm".equals(candidate.getString(MediaFormat.KEY_MIME))){track=i;format=candidate;break;}}
            if(track<0)throw new IOException("No AAC audio track. This tool extracts AAC without transcoding.");ex.selectTrack(track);mux=new MediaMuxer(target(base(f)+"_audio.m4a").getPath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);int dest=mux.addTrack(format);mux.start();started=true;
            ByteBuffer buffer=ByteBuffer.allocate(2*1024*1024);MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();while(true){check();buffer.clear();int n=ex.readSampleData(buffer,0);if(n<0)break;if(n>buffer.capacity())throw new IOException("Audio packet too large");int flags=ex.getSampleFlags();if((flags & (MediaExtractor.SAMPLE_FLAG_ENCRYPTED|MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME))!=0)throw new IOException("Encrypted or partial AAC samples are not supported");info.set(0,n,ex.getSampleTime(),(flags & MediaExtractor.SAMPLE_FLAG_SYNC)!=0?MediaCodec.BUFFER_FLAG_KEY_FRAME:0);mux.writeSampleData(dest,buffer,info);ex.advance();}
        }finally{ex.release();if(mux!=null){try{if(started)mux.stop();}finally{mux.release();}}}
    }
    private void frame(File f,Options o)throws Exception {
        MediaMetadataRetriever m=new MediaMetadataRetriever();try{m.setDataSource(f.getPath());Bitmap b=m.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);if(b==null)throw new IOException("No video frame found");try{saveBitmap(b,target(base(f)+"_frame.jpg"),"jpg",o.quality);}finally{b.recycle();}}finally{m.release();}
    }
}
