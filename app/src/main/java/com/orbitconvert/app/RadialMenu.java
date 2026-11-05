package com.orbitconvert.app;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
/** Six large touch targets per ring. An ordinary list is supplied by the dialog for accessibility. */
public final class RadialMenu extends View {
    public interface Pick { void choose(String action); }
    private final List<String> actions;private final Pick pick;private final Paint paint=new Paint(3);
    private final RectF bounds=new RectF();
    private int page=0;private float radius,cx,cy;
    public RadialMenu(Context c,List<String>a,Pick p){super(c);actions=a;pick=p;setContentDescription("Conversion wheel. Use the action buttons below for screen reader access.");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);cx=getWidth()/2f;cy=getHeight()/2f;radius=Math.min(cx,cy)-10;int count=Math.min(6,actions.size()-page*6);float step=360f/count;
        bounds.set(cx-radius,cy-radius,cx+radius,cy+radius);
        for(int i=0;i<count;i++){paint.setColor(i%2==0?Color.rgb(32,82,70):Color.rgb(48,102,87));c.drawArc(bounds,-90+i*step+2,step-4,true,paint);
            double angle=Math.toRadians(-90+(i+.5)*step);float x=cx+(float)Math.cos(angle)*radius*.70f,y=cy+(float)Math.sin(angle)*radius*.70f;
            paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(getResources().getDisplayMetrics().scaledDensity*12);
            String label=actions.get(page*6+i);String[] words=label.split(" ");if(words.length>1){int middle=label.lastIndexOf(' ');float line=paint.getFontSpacing(),baseline=y-(paint.ascent()+paint.descent())/2;c.drawText(label.substring(0,middle),x,baseline-line/2,paint);c.drawText(label.substring(middle+1),x,baseline+line/2,paint);}else c.drawText(label,x,y-(paint.ascent()+paint.descent())/2,paint);
        }
        paint.setColor(Color.rgb(244,246,242));c.drawCircle(cx,cy,radius*.36f,paint);paint.setColor(Color.rgb(23,61,53));paint.setTextSize(getResources().getDisplayMetrics().scaledDensity*14);c.drawText(actions.size()>6?"More ↻":"Choose",cx,cy+4,paint);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()-cx,y=e.getY()-cy;double distance=Math.hypot(x,y);
        if(distance<radius*.36){page=(page+1)%((actions.size()+5)/6);invalidate();return true;}if(distance>radius)return true;
        double angle=(Math.toDegrees(Math.atan2(y,x))+90+360)%360;int count=Math.min(6,actions.size()-page*6);int index=Math.min(count-1,(int)(angle/(360d/count)));performClick();pick.choose(actions.get(page*6+index));return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
