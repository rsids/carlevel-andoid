package nl.idsklijnsma.carlevel;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nl.idsklijnsma.carlevel.models.UsbItem;

public class UsbDeviceListAdapter extends RecyclerView.Adapter<UsbDeviceListAdapter.ViewHolder> {
    private static final String TAG = "UsbDeviceListAdapter";

    private final List<UsbItem> mDevices;
    private final OnDeviceSelectListener mOnDeviceSelectListener;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (UsbDeviceList ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View v, OnDeviceSelectListener onDeviceSelectListener) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(v1 -> onDeviceSelectListener.onDeviceSelect(getAdapterPosition()));
            textView = (TextView) v.findViewById(R.id.txtBtDevice);
        }

        public TextView getTextView() {
            return textView;
        }
    }
    // END_INCLUDE(recyclerViewSampleViewHolder)

    /**
     * Initialize the dataset of the Adapter.
     */
    public UsbDeviceListAdapter(List<UsbItem> dataSet, OnDeviceSelectListener onDeviceSelectListener) {
        mDevices = dataSet;
        mOnDeviceSelectListener = onDeviceSelectListener;
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.usb_menu_item, viewGroup, false);

        return new ViewHolder(v, mOnDeviceSelectListener);
    }

    @Override
    public void onBindViewHolder(@NonNull UsbDeviceListAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        UsbItem item = mDevices.get(position);
        if(item.driver != null) {
            holder.getTextView().setText(item.driver.getClass().getSimpleName());
        } else {
            holder.getTextView().setText(R.string.unknown_device);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public interface OnDeviceSelectListener {
        void onDeviceSelect(int position);
    }
}
