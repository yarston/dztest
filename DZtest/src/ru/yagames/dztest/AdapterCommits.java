package ru.yagames.dztest;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

/**
 *
 * @author slava
 */
public class AdapterCommits extends ArrayAdapter<CommitModel> {

    private final List<CommitModel> objects;
    private final int resource;
    private final Activity context;

    public AdapterCommits(Context context, int resource, List<CommitModel> objects) {
        super(context, resource, objects);
        this.context = (Activity) context;
        this.resource = resource;
        this.objects = objects;
    }
    
     private final View.OnClickListener ocl = new View.OnClickListener() {

        public void onClick(View v) {
            //ничего)
        }
    };

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        if (rowView == null) {
            rowView = context.getLayoutInflater().inflate(resource, parent, false);
            rowView.setOnClickListener(ocl);
        }
        rowView.setTag(position);
        CommitModel m = objects.get(position);
        CardView cv = (CardView) rowView;
        ((TextView) cv.getChildAt(0)).setText(m.author);
        ((TextView) cv.getChildAt(1)).setText(m.message);
        ((TextView) cv.getChildAt(2)).setText(m.date.replaceAll("[TZ]", " "));
        ((TextView) cv.getChildAt(3)).setText(m.hash);
        return rowView;
    }

}
