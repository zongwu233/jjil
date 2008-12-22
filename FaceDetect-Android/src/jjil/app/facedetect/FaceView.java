package jjil.app.facedetect;

import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FaceView extends ImageView {
	boolean boolShowX = false;
	Rect rectCurrent = null;
	Vector<Rect> vR = new Vector<Rect>();

	public FaceView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public FaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public FaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void addRect(Rect r) {
		vR.add(r);
	}
		
	public boolean hasFaces() {
		return this.vR.size()>0;
	}
	
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (this.boolShowX && this.vR.size() == 0) {
			Paint p = new Paint();
			p.setColor(Color.RED);
			canvas.drawLine(0, 0, this.getWidth(), this.getHeight(), p);
			canvas.drawLine(0, this.getHeight(), this.getWidth(), 0, p);
			return;
		}
		if (this.rectCurrent != null) {
			Paint p = new Paint();
			p.setColor(Color.YELLOW);
			p.setStyle(Style.STROKE);
			canvas.drawRect(this.rectCurrent, p);			
		}
		for (Enumeration<Rect> e = this.vR.elements();e.hasMoreElements();) {
			Rect r = e.nextElement();
			Paint p = new Paint();
			p.setColor(Color.GREEN);
			p.setStyle(Style.STROKE);
			canvas.drawRect(r, p);
		}
	}
	
	public void resetFaces() {
		this.vR.clear();
	}
	
	public void resetShowX() {
		this.boolShowX = false;
	}
	
	public void setCurrentRect(Rect r) {
		this.rectCurrent = r;
	}
	
	public void setShowX() {
		this.boolShowX = true;
	}
}
