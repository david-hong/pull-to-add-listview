package com.pulltoaddlistview.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import java.util.ArrayList;
import com.pulltoaddlistview.app.PullToAddListViewSampleActivity.PullToAddListViewSampleAdapter.ViewHolder;
import com.pulltoaddlistview.app.PullToAddListView.State;

public class PullToAddListViewSampleActivity extends Activity {

    private PullToAddListView listView;
    public PullToAddListViewSampleAdapter adapter;
    private Button add;
    private EditText text;
    // IDs for the context menu actions

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Get the views
        listView = (PullToAddListView) findViewById(R.id.pull_to_add_listview);

        adapter = new PullToAddListViewSampleAdapter() {};
        listView.setAdapter(adapter);

        // Request the adapter to load the data
        adapter.loadData();

        // click listener
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                ViewHolder viewHolder = (ViewHolder) arg1.getTag();
                if (viewHolder.name != null){
                    Toast.makeText(PullToAddListViewSampleActivity.this, viewHolder.name.getText(), Toast.LENGTH_SHORT).show();
                }
                listView.resetHeader();
            }
        });
        this.add = (Button) findViewById(R.id.add);
        this.text = (EditText) findViewById(R.id.ptr_id_text);

        add.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                adapter.items.add(text.getText().toString());
                adapter.notifyDataSetChanged();
                listView.setState(State.PULL_TO_ADD);
                listView.bounceBackHeader();
            }

        });
        // Register the context menu for actions
        registerForContextMenu(listView);
    }


    /**
     * The adapter used to display the results in the list
     *
     */
    public abstract class PullToAddListViewSampleAdapter extends android.widget.BaseAdapter {

        // Strings to be displayed in ListView
        private ArrayList<String> items = new ArrayList<String>();;

        public class ViewHolder {
            public String id;
            public TextView name;
        }

        /**
         * Loads the data.
         */
        public void loadData() {

            items.add("Ajax Amsterdam");
            items.add("Barcelona");
            items.add("Manchester United");
            items.add("Chelsea");
            items.add("Real Madrid");


            // MANDATORY: Notify that the data has changed
            notifyDataSetChanged();
        }

        /*
        Gets number of elements in item list
        */
        @Override
        public int getCount() {
            return items.size();
        }

        /*
        Gets the item at a certain position from the item list
         */
        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        /*
        Gets the item id at a certain position from the item list
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /*
        Gets the view to be displayed
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            String record = (String) getItem(position);

            LayoutInflater inflater = PullToAddListViewSampleActivity.this.getLayoutInflater();

            ViewHolder viewHolder = new ViewHolder();

            if (convertView == null){
                rowView = inflater.inflate(R.layout.list_item,null);

                viewHolder.name = (TextView) rowView.findViewById(R.id.textView1);

                rowView.setTag(viewHolder);
            }

            final ViewHolder holder = (ViewHolder) rowView.getTag();

            holder.name.setText(record);

            return rowView;
        }
    }

}