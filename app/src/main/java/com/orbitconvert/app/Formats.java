package com.orbitconvert.app;
import java.util.*;
public final class Formats {
    public static String ext(String name) { int i=name.lastIndexOf('.'); return i<0?"":name.substring(i+1).toLowerCase(Locale.ROOT); }
    public static boolean image(String e) { return Arrays.asList("jpg","jpeg","png","webp","bmp","gif","heic","heif").contains(e); }
    public static boolean text(String e) { return Arrays.asList("txt","md","log","csv","tsv","json","xml","html","htm","yaml","yml","py","java","js","css").contains(e); }
    public static List<String> actions(List<String> names) {
        List<String> a=new ArrayList<>(); if(names.isEmpty()) return a;
        boolean images=true,pdfs=true,texts=true;
        for(String n:names) { String e=ext(n); images &= image(e); pdfs &= e.equals("pdf"); texts &= text(e); }
        if(images) a.addAll(Arrays.asList("PNG","JPG","WEBP","Images to PDF","Resize","Grayscale","Crop"));
        if(pdfs) a.addAll(Arrays.asList("PDF to PNG","PDF to JPG","PDF to long image","PDF to TXT","Merge PDF","Extract pages","Split PDF","Rasterize PDF"));
        if(texts) a.addAll(Arrays.asList("Text to PDF","Text to DOCX","Text to HTML"));
        if(names.size()==1) {
            String e=ext(names.get(0));
            if(e.equals("zip")) a.add("Extract ZIP");
            if(e.equals("csv")||e.equals("tsv")) a.addAll(Arrays.asList("Table to JSON","CSV","TSV"));
            if(e.equals("json")) a.addAll(Arrays.asList("JSON to CSV","Format JSON"));
            if(e.equals("docx")) a.add("DOCX to TXT");
            if(e.equals("srt")) a.add("SRT to VTT");
            if(e.equals("vtt")) a.add("VTT to SRT");
            if(Arrays.asList("mp4","m4v","mov","3gp","m4a").contains(e)) { a.add("Extract AAC"); if(!e.equals("m4a")) a.add("Video frame"); }
        }
        a.add("Create ZIP"); return a;
    }
}
