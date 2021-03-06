package org.rkoubou.eazyreduction;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class EazyReductionActivity extends Activity implements Constants
{
    private ReductionContext reductionContext;

    static private final String TAG = "EazyReductionActivity";
    static private final int REQUEST_CONFIG = 1;

    static private final int ACTION_DEFAULT = 1;
    private int action = ACTION_DEFAULT;

    //////////////////////////////////////////////////////////////////////////
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.main );

        reductionContext = new ReductionContext( this );

        boolean firstBoot = reductionContext.loadPreference();

        Intent intent = getIntent();
        if( intent != null )
        {
            Uri uri = null;
            if( Intent.ACTION_SEND.equals( intent.getAction() ) )
            {
                uri = intent.getParcelableExtra( Intent.EXTRA_STREAM );
            }
            else
            {
                uri = intent.getData();
            }

            if( uri != null )
            {
                if( !loadImage( uri ) )
                {
                    AlertDialog.Builder b = new AlertDialog.Builder( this );
                    b.setTitle( getString( R.string.error ) );
                    b.setMessage( getString( R.string.failedLoad ) );
                    b.setPositiveButton( getString( R.string.ok ), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            finish();
                        }
                    });
                    b.create().show();
                }
            }
        }

        if( firstBoot )
        {
            AlertDialog.Builder b = new AlertDialog.Builder( this );
            b.setTitle( getString( R.string.welocome ) );
            b.setMessage( getString( R.string.firstMsg1 ) + " '"+ reductionContext.getSaveDir() + "' " + getString( R.string.firstMsg2 ) );
            b.setPositiveButton( getString( R.string.ok ), null );
            b.create().show();
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * コンテキストメニューの設定
     */
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        super.onCreateOptionsMenu( menu );

        final Activity pMe = this;

        MenuItem i;
        i = menu.add( getString( R.string.config ) );
        i.setIcon( android.R.drawable.ic_menu_preferences );
        i.setOnMenuItemClickListener( new OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem mi )
            {
                Intent config = new Intent( pMe, ConfigActivity.class );
                config.putExtra( ReductionContext.KEY_SAVEDIR, reductionContext.getSaveDir() );
                config.putExtra( ReductionContext.KEY_QUORITY, reductionContext.getQuality() );
                config.putExtra( ReductionContext.KEY_FORMAT,  reductionContext.getFormat().name() );
                startActivityForResult( config, REQUEST_CONFIG );
                return true;
            }
        });

        i = menu.add( getString( R.string.app_information ) );
        i.setIcon( android.R.drawable.ic_menu_info_details );
        i.setOnMenuItemClickListener( new OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem mi )
            {
                Intent info = new Intent( pMe, InformationActivity.class );
                startActivity( info );
                return true;
            }
        });

        return true;
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        super.onActivityResult( requestCode, resultCode, data );

        if( requestCode == REQUEST_CONFIG && resultCode == RESULT_OK )
        {
            reductionContext.setSaveDir( data.getExtras().getString( ReductionContext.KEY_SAVEDIR ) );
            reductionContext.setQuality( data.getExtras().getInt( ReductionContext.KEY_QUORITY ) );
            reductionContext.setFormat( CompressFormat.valueOf( CompressFormat.class, data.getExtras().getString( ReductionContext.KEY_FORMAT ) ) );
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        reductionContext.savePreference();
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop()
    {
        super.onStop();
        reductionContext.savePreference();
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        reductionContext.savePreference();
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 1/2ボタン押下イベントハンドラ
     */
    synchronized public void onClickHalfButton( View button )
    {
        resize( 0.5f );
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 1/4ボタン押下イベントハンドラ
     */
    synchronized public void onClickQuarterButton( View button )
    {
        resize( 0.25f );
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 1/8ボタン押下イベントハンドラ
     */
    synchronized public void onClickOneEighth( View button )
    {
        resize( 0.125f );
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 右回転ボタン押下イベントハンドラ
     */
    synchronized public void onClickRotationRightButton( View button )
    {
        rotateBitmap( 90 );
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 左回転ボタン押下イベントハンドラ
     */
    synchronized public void onClickRotationLeftButton( View button )
    {
        rotateBitmap( -90 );
    }



    //////////////////////////////////////////////////////////////////////////
    /**
     * 画像の回転
     */
    synchronized private void rotateBitmap( int rotate )
    {
        if( reductionContext.getCurrentBitmap() == null )
        {
            return;
        }

        Bitmap oldBitmap = reductionContext.getCurrentBitmap();
        try
        {
            Matrix mtx = new Matrix();
            mtx.setRotate( rotate );
            Bitmap newBitmap = Bitmap.createBitmap( oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), mtx, false );

            ( (ImageView)findViewById( R.id.imageView ) ).setImageBitmap( newBitmap );
            reductionContext.setCurrentBitmap( newBitmap );
            oldBitmap.recycle();
        }
        catch( Throwable e )
        {
            e.printStackTrace();
        }

    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 画像のリサイズ、出力
     */
    synchronized private void resize( float scale )
    {
        boolean result;

        Bitmap currentBitmap   = reductionContext.getCurrentBitmap();
        CompressFormat format  = reductionContext.getFormat();
        String saveDir         = reductionContext.getSaveDir();
        String currentFileName = reductionContext.getCurrentFileName();

        String path = ReductionProcessor.makePath( saveDir, currentFileName, currentBitmap, scale, format );

        if( path == null )
        {
            // Unknown Compress format...?
            Toast.makeText( this, getString( R.string.saveNG ), TOAST_SHOW_TIME ).show();
            return;
        }

        // Create a directory if not exists
        File dir = new File( saveDir );
        if( ! dir.exists() && ! dir.mkdir() )
        {
            // Failed to mkdir
            Toast.makeText( this, getString( R.string.saveNG ), TOAST_SHOW_TIME ).show();
            return;
        }

        result = ReductionProcessor.resize( reductionContext, scale );

        if( result )
        {
            String mime = "";
            switch( format )
            {
                case JPEG: mime = "image/jpeg"; break;
                case PNG : mime = "image/png";  break;
                default:
                    Log.w( TAG, "MIME not set. Cannot launch intent!" );
                    return;
            }

            // インテントで画像を渡す
            if( action == ACTION_DEFAULT )
            {
                Uri uri = Uri.fromFile( new File( path ) );
                Intent intent = new Intent( Intent.ACTION_SEND );

                intent.setType( mime );
                intent.putExtra( Intent.EXTRA_STREAM, uri );

                // 外部アプリを起動する
                try
                {
                    startActivity( Intent.createChooser( intent, getString( R.string.shareTitle ) ) );
                }
                catch( ActivityNotFoundException e )
                {
                    Log.e( TAG, "Cannot start Activity", e );
                }
            }
        }
        else
        {
            Toast.makeText( this, getString( R.string.saveNG ), TOAST_SHOW_TIME ).show();
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /**
     * 加工元の画像のロード
     */
    synchronized private boolean loadImage( Uri uri )
    {
        if( uri == null )
        {
            Log.w( TAG, "#loadimage() : uri is null." );
            return false;
        }

        ImageView imageView = (ImageView)findViewById( R.id.imageView );
        try
        {
            Log.d( TAG, "Open from " + uri.toString() );
            Bitmap bitmap;
            String filePath = null;

            if( !uri.getScheme().equals( "file" ) )
            {
                Log.d( TAG, "Image is not received from file. Scheme is " + uri.getScheme() );
                Cursor c = getContentResolver().query( uri, null, null, null, null );
                c.moveToFirst();
                filePath = c.getString( c.getColumnIndex( MediaStore.MediaColumns.DATA ) );
                Log.d( TAG,  "Converted from " + uri.getScheme() +  " to file. filename is " + filePath );
            }
            if( filePath == null )
            {
                filePath = uri.getPath();
            }

            bitmap = MediaStore.Images.Media.getBitmap( this.getContentResolver(), uri );

            if( bitmap != null )
            {
                File path = new File( filePath );
                imageView.setImageBitmap( bitmap );
                reductionContext.setCurrentBitmap( bitmap );
                reductionContext.setCurrentFileName( path.getName().replaceAll( "\\.[^\\,]+$", "" ) );

                // EXIF から回転情報を取得し、Viewに反映
                if( filePath.endsWith( "jpg" ) || filePath.endsWith( "jpeg" ) ||
                    filePath.endsWith( "JPG" ) || filePath.endsWith( "JPEG" ) )
                {
                    try
                    {
                        int degree = 0;

                        ExifInterface exifInterface = new ExifInterface( filePath );
                        int orientation = exifInterface.getAttributeInt( ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED );

                        if( orientation == ExifInterface.ORIENTATION_ROTATE_90 )
                        {
                            degree = 90;
                        }
                        else if( orientation == ExifInterface.ORIENTATION_ROTATE_180 )
                        {
                            degree = 180;
                        }
                        else if( orientation == ExifInterface.ORIENTATION_ROTATE_270 )
                        {
                            degree = 270;
                        }

                        if( degree >= 0 )
                        {
                            rotateBitmap( degree );
                        }
                    }
                    catch( Throwable e )
                    {
                        Log.w( TAG, e );
                    }
                }

                // ウインドウタイトルをファイ名込みにしてみる
                setTitle( getResources().getString( R.string.app_name ) + " - " + path.getName() );
            }

            return true;

        }
        catch( Throwable e )
        {
            Log.e( TAG, "Error", e );
            return false;
        }
    }
}
