package com.android.t;

import android.content.Context;
import android.os.AsyncTask;

import com.android.processes.AndroidProcesses;
import com.android.processes.models.AndroidAppProcess;

import java.util.List;

public class AndroidAppProcessLoader extends AsyncTask<Void, Void, List<AndroidAppProcess>> {

    private final Listener listener;
    private final Context context;

    public AndroidAppProcessLoader(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    @Override
    protected List<AndroidAppProcess> doInBackground(Void... params) {
        List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
        processes.sort((lhs, rhs) -> Utils.getName(context, lhs).compareToIgnoreCase(Utils.getName(context, rhs)));
        return processes;
    }

    @Override
    protected void onPostExecute(List<AndroidAppProcess> androidAppProcesses) {
        listener.onComplete(androidAppProcesses);
    }

    public interface Listener {
        void onComplete(List<AndroidAppProcess> processes);
    }
}