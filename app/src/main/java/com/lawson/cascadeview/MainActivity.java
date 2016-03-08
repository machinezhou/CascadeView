package com.lawson.cascadeview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import com.lawson.library.CascadeView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private List<String> list = new ArrayList<>();
  private DemoCasAdapter demoCasAdapter;
  private CascadeView cascadeView;
  private int mCurrentOffset;
  public final static int DEFAULT_LIMIT = 5;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    cascadeView = (CascadeView) findViewById(R.id.cascade_view);
    cascadeView.setInsets(R.dimen.insets);
  }

  @Override protected void onResume() {
    super.onResume();
    loadData();
  }

  private void loadData() {
    list.clear();
    for (int i = 0; i < DEFAULT_LIMIT; i++) {
      list.add("this is " + i);
    }

    demoCasAdapter = new DemoCasAdapter();
    cascadeView.setAdapter(demoCasAdapter);
    cascadeView.setOnSwipeToLastListener(onScrollListener);
    mCurrentOffset = list.size();
  }

  private void moreData() {
    final int offset = mCurrentOffset + DEFAULT_LIMIT;
    for (int i = mCurrentOffset; i < offset; i++) {
      list.add("this is " + i);
    }

    demoCasAdapter.notifyItemRangeInserted(demoCasAdapter.getItemCount() + DEFAULT_LIMIT,
        DEFAULT_LIMIT);
    mCurrentOffset = offset;
  }

  private CascadeView.OnScrollListener onScrollListener = new CascadeView.OnScrollListener() {
    @Override public void onScrollToLast() {
      super.onScrollToLast();
      moreData();
    }

    @Override public void onScrollToCover() {
      super.onScrollToCover();
      loadData();
    }
  };

  class DemoCasAdapter extends CascadeView.CasAdapter<CasViewHolder> {

    @Override public CasViewHolder onCreateView() {
      View view = getLayoutInflater().inflate(R.layout.layout_items, null, false);
      return new CasViewHolder(view);
    }

    @Override public void onBindViewHolder(CasViewHolder holder, int position) {
      holder.bind(list.get(position));
    }

    @Override public int getItemCount() {
      return list.size();
    }
  }

  class CasViewHolder extends CascadeView.ViewHolder {

    TextView textView;

    public CasViewHolder(View itemView) {
      super(itemView);
      textView = (TextView) itemView.findViewById(R.id.text);
    }

    public void bind(String text) {
      textView.setText(text);
    }
  }
}
