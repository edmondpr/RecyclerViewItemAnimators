package it.gmariotti.recyclerview.itemanimator.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import it.gmariotti.recyclerview.itemanimator.demo.adapter.DividerItemDecoration;
import it.gmariotti.recyclerview.itemanimator.demo.adapter.SimpleAdapter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ExpandableRecyclerView certificationsExpandableRecyclerView;
    RecyclerView mRecyclerView;
    SimpleAdapter mAdapter;
    TextView actionAdd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionAdd = (TextView) findViewById(R.id.actionAdd);
        actionAdd.setOnClickListener(this);

        //Setup RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.expandableList);
        CustomLinearLayoutManager customLayoutManager = new CustomLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(customLayoutManager);

        //mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mAdapter = new SimpleAdapter(this, sCheeseStrings);
        mRecyclerView.setAdapter(mAdapter);

        certificationsExpandableRecyclerView = (ExpandableRecyclerView) findViewById(R.id.certificationsExpandableRecyclerView);
        certificationsExpandableRecyclerView.setText("test");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }


    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.actionAdd:
                mAdapter.add(String.valueOf(mAdapter.getItemCount()), mAdapter.getItemCount());
                break;

            default:
                break;
        }
    }

    public static final String[] sCheeseStrings = {
            "Abbaye de Belloc Abbaye de Belloc Abbaye de Belloc Abbaye de Belloc" , "Abbaye du Mont des Cats Abbaye du Mont des Cats" , "item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 item3 " , "item4 item4 item4 item4 item4 item4 item4 item4 item4 item4 item4 item4 item4 item4 item4 " , "item5" , "item6 item6 item6 item6 item6 item6 item6 item6 item6 item6 item6 item6 item6 item6 item6 "};


}
