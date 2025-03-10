package com.example.soc_macmini_15.musicplayer.Activity;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import com.example.soc_macmini_15.musicplayer.Adapter.ViewPagerAdapter;
import com.example.soc_macmini_15.musicplayer.DB.FavoritesOperations;
import com.example.soc_macmini_15.musicplayer.DB.PlaylistOperations;
import com.example.soc_macmini_15.musicplayer.Fragments.AllSongFragment;
import com.example.soc_macmini_15.musicplayer.Fragments.CurrentSongFragment;
import com.example.soc_macmini_15.musicplayer.Fragments.FavSongFragment;
import com.example.soc_macmini_15.musicplayer.Fragments.PlaylistFragment;
import com.example.soc_macmini_15.musicplayer.Model.Playlist;
import com.example.soc_macmini_15.musicplayer.Model.SongsList;
import com.example.soc_macmini_15.musicplayer.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AllSongFragment.createDataParse, FavSongFragment.createDataParsed, CurrentSongFragment.createDataParsed, PlaylistFragment.createPlaylistDialog {

    private Menu menu;

    private ImageButton imgBtnPlayPause, imgbtnReplay, imgBtnPrev, imgBtnNext, imgBtnSetting, imgBtnShuffle;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SeekBar seekbarController;
    private DrawerLayout mDrawerLayout;
    private TextView tvCurrentTime, tvTotalTime;


    private ArrayList<SongsList> songList;
    private int currentPosition;
    private String searchText = "";
    private SongsList currSong;

    private boolean checkFlag = false, repeatFlag = false, playContinueFlag = false, favFlag = true, playlistFlag = false;
    private final int MY_PERMISSION_REQUEST = 100;
    private int allSongLength;

    MediaPlayer mediaPlayer;
    Handler handler;
    Runnable runnable;

    private PlaylistOperations playlistOperations;
    private Playlist currentPlaylist;

    private boolean shuffleMode = false;
    private ArrayList<Integer> shuffleIndices;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        grantedPermission();

        playlistOperations = new PlaylistOperations(this);
        setPagerLayout();
    }

    /**
     * Initialising the views
     */

    private void init() {
        imgBtnPrev = findViewById(R.id.img_btn_previous);
        imgBtnNext = findViewById(R.id.img_btn_next);
        imgbtnReplay = findViewById(R.id.img_btn_replay);
        imgBtnSetting = findViewById(R.id.img_btn_setting);
        imgBtnShuffle = findViewById(R.id.img_btn_shuffle);

        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        FloatingActionButton refreshSongs = findViewById(R.id.btn_refresh);
        seekbarController = findViewById(R.id.seekbar_controller);
        viewPager = findViewById(R.id.songs_viewpager);
        tabLayout = findViewById(R.id.tabs);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        imgBtnPlayPause = findViewById(R.id.img_btn_play);
        Toolbar toolbar = findViewById(R.id.toolbar);
        handler = new Handler();
        mediaPlayer = new MediaPlayer();

        toolbar.setTitleTextColor(getResources().getColor(R.color.text_color));
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.menu_icon);

        imgBtnNext.setOnClickListener(this);
        imgBtnPrev.setOnClickListener(this);
        imgbtnReplay.setOnClickListener(this);
        refreshSongs.setOnClickListener(this);
        imgBtnPlayPause.setOnClickListener(this);
        imgBtnSetting.setOnClickListener(this);
        imgBtnShuffle.setOnClickListener(this);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                mDrawerLayout.closeDrawers();
                switch (item.getItemId()) {
                    case R.id.nav_about:
                        about();
                        break;
                }
                return true;
            }
        });

        // Optional: Set initial shuffle button state
        if (shuffleMode) {
            imgBtnShuffle.setAlpha(1.0f);
        } else {
            imgBtnShuffle.setAlpha(0.5f);
        }
    }

    /**
     * Function to ask user to grant the permission.
     */

    private void grantedPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
        } else {
            // Initialize ViewPager only after permissions are granted
            if (viewPager != null && tabLayout != null) {
                ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getContentResolver());
                viewPager.setAdapter(adapter);
                tabLayout.setupWithViewPager(viewPager);
            }
        }
    }

    /**
     * Checking if the permission is granted or not
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                        // Initialize ViewPager after permission is granted
                        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getContentResolver());
                        viewPager.setAdapter(adapter);
                        tabLayout.setupWithViewPager(viewPager);
                    }
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }

    /**
     * Function to show the dialog for about us.
     */
    private void about() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about))
                .setMessage(getString(R.string.about_text))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchText = newText;
                queryText();
                setPagerLayout();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(Gravity.START);
                return true;
            case R.id.menu_search:
                Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_favorites:
                if (checkFlag && mediaPlayer != null) {
                    if (favFlag) {  // When song is not yet favorited, add it
                        Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                        item.setIcon(R.drawable.ic_favorite_filled);
                        SongsList favList = new SongsList(
                                songList.get(currentPosition).getTitle(),
                                songList.get(currentPosition).getSubTitle(),
                                songList.get(currentPosition).getPath());
                        FavoritesOperations favoritesOperations = new FavoritesOperations(this);
                        favoritesOperations.addSongFav(favList);
                        setPagerLayout();
                        favFlag = false;
                    } else {  // When already favorited, remove it
                        Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                        item.setIcon(R.drawable.favorite_icon);
                        SongsList currentFav = songList.get(currentPosition);
                        FavoritesOperations favoritesOperations = new FavoritesOperations(this);
                        favoritesOperations.removeSong(currentFav.getPath());
                        setPagerLayout();
                        favFlag = true;
                    }
                }
                return true;
        }

        return super.onOptionsItemSelected(item);

    }


    /**
     * Function to handle the click events.
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_btn_play:
                if (checkFlag) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        imgBtnPlayPause.setImageResource(R.drawable.play_icon);
                    } else if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        imgBtnPlayPause.setImageResource(R.drawable.pause_icon);
                        playCycle();
                    }
                } else {
                    Toast.makeText(this, "Select the Song ..", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_refresh:
                Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show();
                setPagerLayout();
                break;
            case R.id.img_btn_replay:

                if (repeatFlag) {
                    Toast.makeText(this, "Replaying Removed..", Toast.LENGTH_SHORT).show();
                    mediaPlayer.setLooping(false);
                    repeatFlag = false;
                } else {
                    Toast.makeText(this, "Replaying Added..", Toast.LENGTH_SHORT).show();
                    mediaPlayer.setLooping(true);
                    repeatFlag = true;
                }
                break;
            case R.id.img_btn_previous:
                if (checkFlag) {
                    if (mediaPlayer.getCurrentPosition() > 10) {
                        if (currentPosition - 1 > -1) {
                            attachMusic(songList.get(currentPosition - 1).getTitle(), songList.get(currentPosition - 1).getPath());
                            currentPosition = currentPosition - 1;
                        } else {
                            attachMusic(songList.get(currentPosition).getTitle(), songList.get(currentPosition).getPath());
                        }
                    } else {
                        attachMusic(songList.get(currentPosition).getTitle(), songList.get(currentPosition).getPath());
                    }
                } else {
                    Toast.makeText(this, "Select a Song . .", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.img_btn_next:
                if (checkFlag) {
                    if (shuffleMode && shuffleIndices != null) {
                        // Get next shuffled position
                        int currentIndex = shuffleIndices.indexOf(currentPosition);
                        if (currentIndex + 1 < shuffleIndices.size()) {
                            int nextPosition = shuffleIndices.get(currentIndex + 1);
                            attachMusic(songList.get(nextPosition).getTitle(), songList.get(nextPosition).getPath());
                            currentPosition = nextPosition;
                        } else {
                            Toast.makeText(this, "Playlist Ended", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Original next logic
                        if (currentPosition + 1 < songList.size()) {
                            attachMusic(songList.get(currentPosition + 1).getTitle(), songList.get(currentPosition + 1).getPath());
                            currentPosition += 1;
                        } else {
                            Toast.makeText(this, "Playlist Ended", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "Select the Song ..", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.img_btn_setting:
                if (!playContinueFlag) {
                    playContinueFlag = true;
                    Toast.makeText(this, "Loop Added", Toast.LENGTH_SHORT).show();
                } else {
                    playContinueFlag = false;
                    Toast.makeText(this, "Loop Removed", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.img_btn_shuffle:
                toggleShuffle();
                break;
        }
    }

    /**
     * Function to attach the song to the music player
     *
     * @param name
     * @param path
     */

    private void attachMusic(String name, String path) {
        imgBtnPlayPause.setImageResource(R.drawable.play_icon);
        setTitle(name);
        menu.getItem(1).setIcon(R.drawable.favorite_icon);
        favFlag = true;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            setControls();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imgBtnPlayPause.setImageResource(R.drawable.play_icon);
                if (playContinueFlag) {
                    if (currentPosition + 1 < songList.size()) {
                        attachMusic(songList.get(currentPosition + 1).getTitle(), songList.get(currentPosition + 1).getPath());
                        currentPosition += 1;
                    } else {
                        Toast.makeText(MainActivity.this, "PlayList Ended", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // If we're entering shuffle mode with a new song
        if (shuffleMode && shuffleIndices == null) {
            createShuffleIndices();
        }
    }

    /**
     * Function to set the controls according to the song
     */

    private void setControls() {
        seekbarController.setMax(mediaPlayer.getDuration());
        mediaPlayer.start();
        playCycle();
        checkFlag = true;
        if (mediaPlayer.isPlaying()) {
            imgBtnPlayPause.setImageResource(R.drawable.pause_icon);
            tvTotalTime.setText(getTimeFormatted(mediaPlayer.getDuration()));
        }

        seekbarController.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(getTimeFormatted(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Function to play the song using a thread
     */
    private void playCycle() {
        try {
            seekbarController.setProgress(mediaPlayer.getCurrentPosition());
            tvCurrentTime.setText(getTimeFormatted(mediaPlayer.getCurrentPosition()));
            if (mediaPlayer.isPlaying()) {
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        playCycle();

                    }
                };
                handler.postDelayed(runnable, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTimeFormatted(long milliSeconds) {
        String finalTimerString = "";
        String secondsString;

        //Converting total duration into time
        int hours = (int) (milliSeconds / 3600000);
        int minutes = (int) (milliSeconds % 3600000) / 60000;
        int seconds = (int) ((milliSeconds % 3600000) % 60000 / 1000);

        // Adding hours if any
        if (hours > 0)
            finalTimerString = hours + ":";

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10)
            secondsString = "0" + seconds;
        else
            secondsString = "" + seconds;

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // Return timer String;
        return finalTimerString;
    }


    /**
     * Function Overrided to receive the data from the fragment
     *
     * @param name
     * @param path
     */

    @Override
    public void onDataPass(String name, String path) {
        if (name != null && path != null) {
            currentPosition = -1;
            attachMusic(name, path);
        }
    }

    @Override
    public void getLength(int length) {
        this.allSongLength = length;
    }

    @Override
    public void fullSongList(ArrayList<SongsList> songList, int position) {
        this.songList = songList;
        this.currentPosition = position;
        this.playlistFlag = false;
        
        if (shuffleMode) {
            createShuffleIndices();
        }
    }

    @Override
    public String queryText() {
        return searchText;
    }

    @Override
    public void currentSong(SongsList songsList) {
        this.currSong = songsList;
    }

    @Override
    public int getPosition() {
        return currentPosition;
    }

    @Override
    public SongsList getSong() {
        return currSong;
    }

    @Override
    public boolean getPlaylistFlag() {
        return playlistFlag;
    }

    @Override
    public void onPlaylistSelected(Playlist playlist) {
        currentPlaylist = playlist;
        ArrayList<SongsList> playlistSongs = playlistOperations.getPlaylistSongs(playlist.getId());
        if (!playlistSongs.isEmpty()) {
            onDataPass(playlistSongs.get(0).getTitle(), playlistSongs.get(0).getPath());
            fullSongList(playlistSongs, 0, true);
        }
    }

    private void addSongToPlaylist(SongsList song) {
        if (currentPlaylist != null) {
            playlistOperations.addSongToPlaylist(currentPlaylist.getId(), song);
            Toast.makeText(this, "Song added to playlist", Toast.LENGTH_SHORT).show();
        }
    }

    public void showOptionsDialog(final int position, final ArrayList<SongsList> songs) {
        final SongsList selectedSong = songs.get(position);
        String[] options = {"Play Next", "Add to Playlist"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // Play next
                        if (songs != null && position < songs.size()) {
                            currentSong(selectedSong);
                            updateUI();
                        }
                        break;
                    case 1:
                        // Show playlist selection dialog
                        showPlaylistSelectionDialog(selectedSong);
                        break;
                }
            }
        });
        builder.show();
    }

    private void showPlaylistSelectionDialog(final SongsList song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Playlist");
        
        final ArrayList<Playlist> playlists = playlistOperations.getPlaylists();
        if (playlists.isEmpty()) {
            // Show dialog to create new playlist
            AlertDialog.Builder createPlaylistBuilder = new AlertDialog.Builder(this);
            createPlaylistBuilder.setTitle("No Playlists");
            createPlaylistBuilder.setMessage("Would you like to create a new playlist?");
            
            createPlaylistBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showCreatePlaylistDialog(song);
                }
            });
            
            createPlaylistBuilder.setNegativeButton("Cancel", null);
            createPlaylistBuilder.show();
            return;
        }
        
        String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            playlistNames[i] = playlists.get(i).getName();
        }
        
        builder.setItems(playlistNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Playlist selectedPlaylist = playlists.get(which);
                playlistOperations.addSongToPlaylist(selectedPlaylist.getId(), song);
                Toast.makeText(MainActivity.this, 
                    "Added to " + selectedPlaylist.getName(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setPositiveButton("New Playlist", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showCreatePlaylistDialog(song);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCreatePlaylistDialog(final SongsList songToAdd) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Playlist");

        final EditText input = new EditText(this);
        input.setHint("Playlist name");
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String playlistName = input.getText().toString();
                if (!playlistName.isEmpty()) {
                    playlistOperations.createPlaylist(playlistName);
                    // Get the newly created playlist
                    ArrayList<Playlist> playlists = playlistOperations.getPlaylists();
                    if (!playlists.isEmpty()) {
                        Playlist newPlaylist = playlists.get(playlists.size() - 1);
                        // Add the song to the new playlist
                        playlistOperations.addSongToPlaylist(newPlaylist.getId(), songToAdd);
                        Toast.makeText(MainActivity.this, 
                            "Created playlist '" + playlistName + "' and added song", 
                            Toast.LENGTH_SHORT).show();
                        // Refresh the playlist fragment if it's visible
                        updateUI();
                    }
                } else {
                    Toast.makeText(MainActivity.this, 
                        "Please enter a playlist name", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateUI() {
        if (viewPager != null) {
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    private void setPagerLayout() {
        if (viewPager != null && tabLayout != null) {
            ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getContentResolver());
            viewPager.setAdapter(adapter);
            tabLayout.setupWithViewPager(viewPager);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }
    }

    private void toggleShuffle() {
        shuffleMode = !shuffleMode;
        if (shuffleMode) {
            // Visual feedback - make button more opaque when active
            imgBtnShuffle.setAlpha(1.0f);
            createShuffleIndices();
        } else {
            // Make button more transparent when inactive
            imgBtnShuffle.setAlpha(0.5f);
        }
    }

    private void createShuffleIndices() {
        shuffleIndices = new ArrayList<>();
        for (int i = 0; i < songList.size(); i++) {
            shuffleIndices.add(i);
        }
        Collections.shuffle(shuffleIndices);
    }

    @Override
    public void onDataParsed(String name, String path) {
        if (name != null && path != null) {
            currentPosition = -1;
            attachMusic(name, path);
        }
    }

    @Override
    public void fullSongList(ArrayList<SongsList> songsList, int position, boolean playlistFlag) {
        this.songList = songsList;
        this.currentPosition = position;
        this.playlistFlag = playlistFlag;
        
        if (shuffleMode) {
            createShuffleIndices();
        }
    }
}
