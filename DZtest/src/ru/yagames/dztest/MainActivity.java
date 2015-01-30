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
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import static android.view.View.GONE;
import android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import static android.view.animation.Animation.RELATIVE_TO_PARENT;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private final ArrayList<RepoModel> repoList = new ArrayList<RepoModel>();
    private final ArrayList<CommitModel> commitList = new ArrayList<CommitModel>();
    private ListView repoLV;
    private ListView commitLV;
    private ViewFlipper flipper;
    private AdapterRepos adapterRepo;
    private String login;
    private String password;
    private static SharedPreferences.Editor editor;
    private static SharedPreferences mSettings;
    private final static String APP_PREFERENCES = "mysettings";
    private boolean needAuth;
    private AdapterCommits adapterCommits;
    private ProgressBar progressBar;
    private boolean commitsShown;
    private TranslateAnimation flipOutBackward;
    private TranslateAnimation flipInBackward;
    private TranslateAnimation flipOutForward;
    private TranslateAnimation flipInForward;
    private ImageView back;
    private TextView head;
    private final static String PREF_TOKEN = "token";
    private String token;
    private CardView cv;
   // private CardView cv;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTranslucent(this, 0xFFFF9700);
        setContentView(R.layout.main);
        back = (ImageView) findViewById(R.id.iv);
        back.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                showDialog(2);
            }
        });
        cv = new CardView(this);
        head = (TextView) findViewById(R.id.head);
        head.setText(R.string.head_repo);
       // cv = new CardView(this);
        progressBar = (ProgressBar) findViewById(R.id.pbar);
        flipper = (ViewFlipper) findViewById(R.id.flipper);
        repoLV = (ListView) findViewById(R.id.repo_list);
        commitLV = (ListView) findViewById(R.id.commits_list);
        adapterRepo = new AdapterRepos(this, R.layout.item_repo, repoList);
        adapterCommits = new AdapterCommits(this, R.layout.item_commit, commitList);
        repoLV.setAdapter(adapterRepo);
        commitLV.setAdapter(adapterCommits);
        flipInForward = new TranslateAnimation(RELATIVE_TO_PARENT, 1, RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 0);
        flipOutForward = new TranslateAnimation(RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, -1, RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 0);
        flipInBackward = new TranslateAnimation(RELATIVE_TO_PARENT, -1, RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 0);
        flipOutBackward = new TranslateAnimation(RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 1, RELATIVE_TO_PARENT, 0, RELATIVE_TO_PARENT, 0);
        AccelerateDecelerateInterpolator adi = new AccelerateDecelerateInterpolator();
        for (TranslateAnimation ta : new TranslateAnimation[]{flipInForward, flipOutForward, flipInBackward, flipOutBackward}) {
            ta.setDuration(500);
            ta.setInterpolator(adi);
        }
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        token = mSettings.getString(PREF_TOKEN, null);
        if (token == null) {
            needAuth = true;
            showDialog(1);
        } else {
            getRepoList();
        }
        Log.v("dztest", "token=" + token + "; login=" + login + "; password=" + password);
    }

    private void oAuth() {
        //проверка на то, что авторизация уже существует
         progressBar.setVisibility(VISIBLE);
        Get get = new Get("https://api.github.com/authorizations");
        get.setAuthParam(login, password);
        get.setResponseListener(new ResponseListener() {

            public void onResponseEvent(String line) {
                try {
                    JSONArray jarr = new JSONArray(line);
                    int length = jarr.length();
                    if (length > 0) {
                        for (int i = 0; i < length; i++) {
                            JSONObject jo = jarr.getJSONObject(i);
                            if (jo.getString("note").equals("test")) { //конечно, вместо test хорошо бы взять более уникальную метку
                                //login = null;
                                //password = null;
                                token = jo.getString("token");
                                saveSettings();
                                break;
                            }
                        }
                    }
                } catch (JSONException ex) {
                }
                //если не существует, создать
                if (token == null) {
                    Post post = new Post("https://api.github.com/authorizations");
                    post.setAuthParam(login, password);
                    post.setResponseListener(new ResponseListener() {

                        public void onResponseEvent(String line) {
                            try {
                                JSONObject jobj = new JSONObject(line);
                                token = jobj.getString("token");
                                if (token != null && token.length() > 0) {
                                    needAuth = false;
                                    //  login = null;
                                    //    password = null;
                                    saveSettings();
                                     progressBar.setVisibility(GONE);
                                     getRepoList();
                                }
                            } catch (JSONException ex) {
                            }
                            getRepoList();
                        }
                    });
                    post.execute();
                } else {
                     progressBar.setVisibility(GONE);
                     getRepoList();
                }
            }
        });
        get.setConnectionErrorListener(cel);
        get.execute();
    }

    public static SystemBarTintManager setTranslucent(Activity activity, int color) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        win.setAttributes(winParams);
        SystemBarTintManager mTintManager = new SystemBarTintManager(activity);
        mTintManager.setStatusBarTintEnabled(true);
        mTintManager.setTintColor(color);
        return mTintManager;
    }

    @Override
    public void onBackPressed() {
        if (commitsShown) {
            flipper.setInAnimation(flipInBackward);
            flipper.setOutAnimation(flipOutBackward);
            flipper.showPrevious();
            commitsShown = false;
            head.setText(R.string.head_repo);
        } else {
            super.onBackPressed();
        }
    }

    protected void showCommits(int position) {
        progressBar.setVisibility(VISIBLE);
        RepoModel m = repoList.get(position);
        Get get = new Get("https://api.github.com/repos/" + m.owner + "/" + m.name + "/commits");
        // if (token == null) {
        //     get.setAuthParam(login, password);
        // } else {
        get.put("access_token", token);
        //}
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
                flipper.setInAnimation(flipInForward);
                flipper.setOutAnimation(flipOutForward);
                flipper.showNext();
                commitsShown = true;
                head.setText(R.string.head_commits);
            }
        });
        get.execute();
    }

    private void saveSettings() {
        editor = mSettings.edit();
        // editor.putString(PREF_USERNAME, login);
        // editor.putString(PREF_PASS, password);
        editor.putString(PREF_TOKEN, token);
        editor.commit();
    }

    private void getRepoList() {
        progressBar.setVisibility(VISIBLE);
        Get get = new Get("https://api.github.com/user/repos");
        // if (token == null) {
        //get.setAuthParam(login, password);
        // } else {
        get.put("access_token", token);
        //}
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
                    showErr(2);
                }
                adapterRepo.notifyDataSetChanged();
                progressBar.setVisibility(GONE);
            }
        });
        get.setConnectionErrorListener(cel);
        get.execute();
    }

    private ConnectionErrorListener cel = new ConnectionErrorListener() {

        public void onConnectionError(int errCode) {
            showErr(errCode);
        }
    };

    private void showErr(int errCode) {
        Toast.makeText(MainActivity.this, errCode == 1 ? "Ошибка соединения с интернет." : "Ошибка авторизации. Попробуйте повторить попытку.", Toast.LENGTH_SHORT).show();
        if (needAuth) {
            showDialog(1);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (id == 1) {
            //builder.setCancelable(false);
            builder.setTitle(R.string.dialog_label);
            final View v = getLayoutInflater().inflate(R.layout.dialog_login, null);
            builder.setView(v);
            builder.setPositiveButton(R.string.dialog_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    LinearLayout ll = ((LinearLayout) v);
                    login = ((EditText) ll.getChildAt(1)).getText().toString();
                    password = ((EditText) ll.getChildAt(2)).getText().toString();
                    //getRepoList();
                    oAuth();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    System.exit(0);
                }
            });
        } else {
            builder.setTitle(R.string.logout_label);
            builder.setMessage(R.string.logout_info);
            builder.setPositiveButton(R.string.logout_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    //     login = null;
                    //     password = null;
                    token = null;
                    needAuth = true;
                    saveSettings();
                    showDialog(1);
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    dialog.cancel();
                }
            });
        }
        return builder.create();
    }

}
