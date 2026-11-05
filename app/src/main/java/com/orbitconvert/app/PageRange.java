package com.orbitconvert.app;
import java.util.*;
/** Adapted from Filee.Core/Conversion/PageRange.cs (MIT). See FILEE-LICENSE.txt.
 * Android port deliberately rejects invalid input rather than silently exporting all pages. */
public final class PageRange {
    public static List<Integer> parse(String expression, int count) {
        if (count < 1) throw new IllegalArgumentException("Document has no pages");
        StringBuilder normalized = new StringBuilder();
        for (char c : expression.toCharArray()) {
            if (Character.isDigit(c)) normalized.append(Character.digit(c, 10));
            else if ("–—−－".indexOf(c)>=0) normalized.append('-');
            else if ("，、،".indexOf(c)>=0) normalized.append(',');
            else normalized.append(c);
        }
        String s=normalized.toString().trim();
        TreeSet<Integer> pages=new TreeSet<>();
        if (s.isEmpty()) { for(int i=0;i<count;i++) pages.add(i); }
        else for(String part:s.split(",",-1)) {
            part=part.trim();
            if (!part.matches("[0-9]+|[0-9]*\\s*-\\s*[0-9]*") || part.equals("-")) throw new IllegalArgumentException("Invalid page range: "+part);
            String[] r=part.split("-",-1);
            int a,b;
            try { a=r[0].trim().isEmpty()?1:Integer.parseInt(r[0].trim()); b=r.length==1?a:(r[1].trim().isEmpty()?count:Integer.parseInt(r[1].trim())); }
            catch(NumberFormatException e) { throw new IllegalArgumentException("Page number is too large"); }
            if(a<1||b<1||a>count||b>count) throw new IllegalArgumentException("Pages must be between 1 and "+count);
            for(int i=Math.min(a,b);i<=Math.max(a,b);i++) pages.add(i-1);
        }
        return new ArrayList<>(pages);
    }
}
