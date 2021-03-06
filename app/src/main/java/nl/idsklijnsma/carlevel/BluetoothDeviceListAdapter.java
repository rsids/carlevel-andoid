package nl.idsklijnsma.carlevel;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BluetoothDeviceListAdapter extends RecyclerView.Adapter<BluetoothDeviceListAdapter.ViewHolder> {
    private static final String TAG = "BluetoothDeviceListAdapter";

    private List<BluetoothDevice> mDevices;
    private OnDeviceSelectListener mOnDeviceSelectListener;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (BluetoothDeviceList ViewHolder)
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
    public BluetoothDeviceListAdapter(List<BluetoothDevice> dataSet, OnDeviceSelectListener onDeviceSelectListener) {
        mDevices = dataSet;
        mOnDeviceSelectListener = onDeviceSelectListener;
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.bluetooth_menu_item, viewGroup, false);

        return new ViewHolder(v, mOnDeviceSelectListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothDeviceListAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        holder.getTextView().setText(mDevices.get(position).getName());
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
