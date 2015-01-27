package ru.yagames.dztest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewFlipper;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private ArrayList<RepoModel> repoList = new ArrayList<RepoModel>();
    private ArrayList<CommitModel> commitList = new ArrayList<CommitModel>();
    private ListView repoLV;
    private ListView commitLV;
    private ViewFlipper flipper;
    private AdapterRepos adapterRepo;
    private String login;
    private String password;
    private static SharedPreferences.Editor editor;
    private static SharedPreferences mSettings;
    private final static String APP_PREFERENCES = "mysettings";
    private final static String PREF_USERNAME = "user";
    private final static String PREF_PASS = "pass";
    private boolean needAuth;
    private AdapterCommits adapterCommits;
    private ProgressBar progressBar;
    private boolean commitsShown;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        editor = mSettings.edit();
        login = mSettings.getString(PREF_USERNAME, null);
        password = mSettings.getString(PREF_PASS, null);
        progressBar = (ProgressBar) findViewById(R.id.pbar);
        flipper = (ViewFlipper) findViewById(R.id.flipper);
        repoLV = (ListView) findViewById(R.id.repo_list);
        commitLV = (ListView) findViewById(R.id.commits_list);
        adapterRepo = new AdapterRepos(this, R.layout.item_repo, repoList);
        adapterCommits = new AdapterCommits(this, R.layout.item_commit, commitList);
        repoLV.setAdapter(adapterRepo);
        commitLV.setAdapter(adapterCommits);
        if (login == null || password == null) {
            needAuth = true;
            showDialog(0);
        } else {
            getRepoList();
        }
    }

    @Override
    public void onBackPressed() {
        if (commitsShown) {
            flipper.showPrevious();
        } else {
            super.onBackPressed();
        }
    }

    protected void showCommits(int position) {
        progressBar.setVisibility(VISIBLE);
        RepoModel m = repoList.get(position);
        Get get = new Get("https://api.github.com/repos/" + m.owner + "/" + m.name + "/commits");
        get.setAuthParam(login, password);
        get.setResponseListener(new ResponseListener() {

            public void onResponseEvent(String line) {
                commitList.clear();
                try {
                    JSONArray jarr = new JSONArray(line);
                    int length = jarr.length();
                    if (length > 0) {
                        for (int i = 0; i < length; i++) {
                            JSONObject jobj = jarr.getJSONObject(i);
                            JSONObject commit = jobj.getJSONObject("commit");
                            JSONObject committer = commit.getJSONObject("committer");
                            CommitModel cm = new CommitModel();
                            cm.hash = jobj.getString("sha");
                            cm.author = committer.getString("name");
                            cm.date = committer.getString("date");
                            cm.message = commit.getString("message");
                            commitList.add(cm);
                        }
                    }
                } catch (JSONException ex) {

                }
                adapterCommits.notifyDataSetChanged();
                progressBar.setVisibility(GONE);
                flipper.showNext();
                commitsShown = true;
            }
        });
        get.execute();
    }

    private void saveSettings() {
        editor = mSettings.edit();
        editor.putString(PREF_USERNAME, login);
        editor.putString(PREF_PASS, password);
        editor.commit();
    }

    private void getRepoList() {
        progressBar.setVisibility(VISIBLE);
        Get get = new Get("https://api.github.com/user/repos");
        get.setAuthParam(login, password);
        get.setResponseListener(new ResponseListener() {

            public void onResponseEvent(String line) {
                repoList.clear();
                try {
                    JSONArray jarr = new JSONArray(line);
                    int length = jarr.length();
                    if (length > 0) {
                        for (int i = 0; i < length; i++) {
                            JSONObject jobj = jarr.getJSONObject(i);
                            JSONObject owner = jobj.getJSONObject("owner");
                            RepoModel r = new RepoModel();
                            r.name = jobj.getString("name");
                            r.description = jobj.getString("description");
                            r.owner = owner.getString("login");
                            r.avatar = owner.getString("avatar_url");
                            r.watches = jobj.getInt("watchers");
                            r.forks = jobj.getInt("forks");
                            repoList.add(r);
                        }
                    }
                    if (needAuth) {
                        saveSettings();
                        needAuth = false;
                    }
                } catch (JSONException ex) {
                }
                adapterRepo.notifyDataSetChanged();
                progressBar.setVisibility(GONE);
            }
        });
        get.setConnectionErrorListener(new ConnectionErrorListener() {

            public void onConnectionError(int errCode) {
                Toast.makeText(MainActivity.this, errCode == 1 ? "Ошибка соединения с интернет." : "Ошибка авторизации. Попробуйте повторить попытку.", Toast.LENGTH_SHORT).show();
                if (needAuth) {
                    showDialog(0);
                }
            }
        });
        get.execute();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setCancelable(false);
        builder.setTitle(R.string.dialog_label);
        final View v = getLayoutInflater().inflate(R.layout.dialog_login, null);
        builder.setView(v);
        builder.setPositiveButton(R.string.dialog_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                LinearLayout ll = ((LinearLayout) v);
                login = ((EditText) ll.getChildAt(1)).getText().toString();
                password = ((EditText) ll.getChildAt(2)).getText().toString();
                getRepoList();
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                System.exit(0);
            }
        });
        return builder.create();
    }

}
