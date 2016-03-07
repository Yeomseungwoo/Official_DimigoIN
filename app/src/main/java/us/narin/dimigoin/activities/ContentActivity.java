package us.narin.dimigoin.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bumptech.glide.Glide;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import us.narin.dimigoin.R;
import us.narin.dimigoin.adapter.BoardCommentAdapter;
import us.narin.dimigoin.api.ApiObject;
import us.narin.dimigoin.api.ApiRequests;
import us.narin.dimigoin.model.pojo.ContentDetail;
import us.narin.dimigoin.model.pojo.File;
import us.narin.dimigoin.util.NestedInRecyclerManager;
import us.narin.dimigoin.util.Schema;
import us.narin.dimigoin.util.Session;
import us.narin.dimigoin.util.TimeStamp;

import java.io.IOException;
import java.util.List;

public class ContentActivity extends AppCompatActivity {

    private static final String TAG = "ContentActivity";

    @Bind(R.id.content_detail_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.bbs_content_detail_content)
    WebView contentView;
    @Bind(R.id.bbs_content_detail_author)
    TextView contentAuthor;
    @Bind(R.id.bbs_content_detail_time)
    TextView contentTime;
    @Bind(R.id.bbs_content_detail_profile)
    Button contentProfile;
    @Bind(R.id.bbs_content_detail_file_1)
    TextView firstFile;
    @Bind(R.id.bbs_content_detail_file_2)
    TextView secondFile;
    @Bind(R.id.bbs_content_detail_file_1_wrapper)
    CardView firstFileWrapper;
    @Bind(R.id.bbs_content_detail_file_2_wrapper)
    CardView secondFileWrapper;
    @Bind(R.id.bbs_content_detail_like_count)
    TextView likeCount;
    @Bind(R.id.bbs_content_detail_view_count)
    TextView viewCount;
    @Bind(R.id.bbs_content_detail_comment_count)
    TextView commentCount;
    @Bind(R.id.bbs_content_detail_comment_rv)
    RecyclerView commentView;
    @Bind(R.id.bbs_content_detail_comment_wrappper)
    CardView commentWrapper;

    @Bind(R.id.bbs_content_detail_comment_field)
    EditText commentField;
    @Bind(R.id.bbs_content_detail_comment_submit)
    Button commentSubmit;

    @BindString(R.string.bbs_comment)
    String bbsComment;
    @BindString(R.string.bbs_like)
    String bbsLike;
    @BindString(R.string.bbs_view)
    String bbsView;
    @BindString(R.string.bbs_unit)
    String bbsUnit;

    private Integer contentId;
    private String boardId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        Intent mIntent = getIntent();

        contentId = mIntent.getIntExtra("content_id", 0);
        String contentSubject = mIntent.getStringExtra("content_subject");
        boardId = mIntent.getStringExtra("content_board");
        String userToken = Session.getUserToken(getApplicationContext());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(contentSubject);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Glide.with(getApplicationContext()).load(R.drawable.nav_bg).into((ImageView) findViewById(R.id.content_bg));

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final WebSettings webSettings = contentView.getSettings();
//        webSettings.setJavaScriptEnabled(true);

        contentView.setBackgroundColor(0);
        contentView.setLongClickable(false);
        webSettings.setDefaultFontSize(14);
//        webSettings.setDomStorageEnabled(true);

        ApiRequests apiRequests = ApiObject.initClient(Schema.API_ENDPOINT);
        Call<ContentDetail> callDetail = apiRequests.getContentDetail(boardId, contentId, userToken);

        callDetail.enqueue(new Callback<ContentDetail>() {

            @Override
            public void onResponse(Response<ContentDetail> response) {
                //JSON 응답 (타입오류)의 이상으로 제일 첫번째의 데이터를 가져옵니다.
                final ContentDetail.Data tmpModel = response.body().getResultData().get(0);
                final List<File> fileList = response.body().getResultData().get(0).getFiles();
                contentView.loadData(Schema.WEBVIEW_DEFAULT_STYLE + tmpModel.getArticle().getContentBody(), Schema.WEBVIEW_DEFAULT_TYPE, null);

                contentAuthor.setText(tmpModel.getArticle().getAuthorName());
                contentTime.setText(new TimeStamp(getApplicationContext()).getTimes(tmpModel.getArticle().getPostTime()));
                contentProfile.setText(String.valueOf(tmpModel.getArticle().getAuthorName().charAt(0)));

                likeCount.setText(bbsLike + tmpModel.getArticle().getGoodCount() + bbsUnit);
                commentCount.setText(bbsComment + tmpModel.getComments().size() + bbsUnit);
                viewCount.setText(bbsView + tmpModel.getArticle().getViewCount() + bbsUnit);
                if (!tmpModel.getFiles().isEmpty()) {
                    if (fileList.size() == 1) {
                        firstFileWrapper.setVisibility(View.VISIBLE);
                        firstFile.setText(fileList.get(0).getFileName() + "[" + fileList.get(0).getDownloadCount() + "]");
                        firstFileWrapper.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Schema.FILE_DOWNLOAD + fileList.get(0).getFilePath() + "/" + Session.getUserToken(getApplicationContext())))));
                    } else if (fileList.size() == 2) {
                        secondFileWrapper.setVisibility(View.VISIBLE);
                        secondFile.setText(fileList.get(1).getFileName() + "[" + fileList.get(1).getDownloadCount() + "]");
                        secondFileWrapper.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Schema.FILE_DOWNLOAD + fileList.get(1).getFilePath() + "/" + Session.getUserToken(getApplicationContext())))));
                    }
                }

                if (!tmpModel.getComments().isEmpty()) {
                    commentWrapper.setVisibility(View.VISIBLE);
                    //fix NestiedScroollView in RecyclerView
                    commentView.setLayoutManager(new NestedInRecyclerManager(getApplicationContext()));
                    commentView.setHasFixedSize(false);
                    commentView.setNestedScrollingEnabled(false);

                    BoardCommentAdapter commentAdapter = new BoardCommentAdapter(getApplicationContext(), response.body().getResultData().get(0).getComments());
                    commentView.setAdapter(commentAdapter);

                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("ContentActivity 에러", t.getMessage());
            }
        });


    }

    @OnClick(R.id.bbs_content_detail_comment_submit)
    void onSubmitComment() {
        final String commentContent = commentField.getText().toString();

        new Thread(() -> {
            try {
                Connection.Response response = Jsoup.connect(Schema.ENDPOINT + "/bbs/write_comment_update.php")
                        .cookie(Schema.LOGIN_COOKIE_KEY, Session.getUserCookie(getApplicationContext()))
                        .data("wr_id", contentId.toString())
                        .data("wr_content", commentContent)
                        .data("w", "c")
                        .data("bo_table", boardId).method(Connection.Method.POST)
                        .execute();

                if (response.statusCode() == 200) {
                    //댓글 작성 성공
                } else {
                    //댓글 작성 실패
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}
