package ru.yagames.dztest;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import static android.view.MotionEvent.ACTION_DOWN;
import android.view.View;
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

    private final View.OnTouchListener otl = new View.OnTouchListener() {

        public boolean onTouch(View arg0, MotionEvent arg1) {
            if (arg1.getAction() == ACTION_DOWN) {
                ((MainActivity) context).showCommits((Integer) arg0.getTag());
            }
            return true;
        }
    };

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        if (rowView == null) {
            rowView = context.getLayoutInflater().inflate(resource, parent, false);
            rowView.setOnTouchListener(otl);
        }
        rowView.setTag(position);
        RepoModel m = objects.get(position);
        FrameLayout fl = (FrameLayout) rowView;
        ImageProcessor.load(m.avatar, ((ImageView) fl.getChildAt(0)));
        ((TextView) fl.getChildAt(1)).setText(m.name);
        ((TextView) fl.getChildAt(2)).setText(m.owner);
        ((TextView) fl.getChildAt(3)).setText(m.description);
        LinearLayout ll = (LinearLayout) fl.getChildAt(4);
        ((TextView) ll.getChildAt(1)).setText(Integer.toString(m.forks));
        ((TextView) ll.getChildAt(3)).setText(Integer.toString(m.watches));
        return rowView;
    }
}
