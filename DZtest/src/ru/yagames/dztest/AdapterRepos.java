package ru.yagames.dztest;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.MotionEvent;
import static android.view.MotionEvent.ACTION_DOWN;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 *
 * @author slava
 */
public class AdapterRepos extends ArrayAdapter<RepoModel> {

    private final Activity context;
    private final int resource;
    private final List<RepoModel> objects;

    public AdapterRepos(Context context, int resource, List<RepoModel> objects) {
        super(context, resource, objects);
        this.context = (Activity) context;
        this.resource = resource;
        this.objects = objects;
    }
    
    private final OnClickListener ocl = new OnClickListener() {

        public void onClick(View v) {
            ((MainActivity) context).showCommits((Integer) v.getTag());
        }
    };

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        if (rowView == null) {
            rowView = context.getLayoutInflater().inflate(resource, parent, false);
            rowView.setOnClickListener(ocl);
        }
        rowView.setTag(position);
        RepoModel m = objects.get(position);
        CardView cv = (CardView) rowView;
        ImageProcessor.load(m.avatar, ((ImageView) cv.getChildAt(0)));
        ((TextView) cv.getChildAt(1)).setText(m.name);
        ((TextView) cv.getChildAt(2)).setText(m.owner);
        ((TextView) cv.getChildAt(3)).setText(m.description);
        LinearLayout ll = (LinearLayout) cv.getChildAt(4);
        ((TextView) ll.getChildAt(1)).setText(Integer.toString(m.forks));
        ((TextView) ll.getChildAt(3)).setText(Integer.toString(m.watches));
        return rowView;
    }
}
