package jp.itnav.derushio.btmarubatsugame;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import jp.itnav.derushio.btmanager.BtServerManagedActivity;
import jp.itnav.derushio.btmanager.timer.TimerHandler;


public class MainActivity extends BtServerManagedActivity {

	private static final String MY_TURN = "あなたの番です";
	private static final String YOUR_TURN = "あいての番です";

	private boolean mServerMode;

	private LinearLayout mGameDisplay;
	private TimerHandler mReadServerMessageTimer;
	private TimerHandler mReadMessageTimer;

	private TextView mTextViewStatus;

	Button[][] mButtonCell = new Button[3][3];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGameDisplay = (LinearLayout) findViewById(R.id.layout_game_display);
		mTextViewStatus = (TextView) findViewById(R.id.text_status);
		// xmlと関連づけ

		for (int j = 0; j < 3; j++) {
			LinearLayout horizontalLayout = new LinearLayout(this);
			horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
			horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
			for (int k = 0; k < 3; k++) {
				mButtonCell[j][k] = new Button(this);
				mButtonCell[j][k].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
				mButtonCell[j][k].setTag("0P");
				final String position = j + "," + k + "\n";
				mButtonCell[j][k].setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mTextViewStatus.getText().toString().equals(MY_TURN)) {
							if (v.getTag().equals("0P")) {
								v.setBackgroundColor(Color.BLUE);
								v.setTag("あなた");

								if (mServerMode) {
									writeServerMessage(position);
								} else {
									writeMessage(position);
								}

								mTextViewStatus.setText(YOUR_TURN);
								checkCell();
							}
						}
					}
				});

				horizontalLayout.addView(mButtonCell[j][k]);
			}

			mGameDisplay.addView(horizontalLayout);
		}
		// ゲーム画面を描画

		mReadServerMessageTimer = new TimerHandler();
		mReadServerMessageTimer.setOnTickListener(new TimerHandler.OnTickListener() {
			@Override
			public void onTick() {
				if (isServerSocketExist()) {
					ArrayList<String> messages = getServerMessageMailBox();
					messageParse(messages.get(0));
				}
			}
		});
		// サーバーのメッセージボックスを定期的に取得する設定（まだスタートしない）

		setOnServerConnectAction(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				if (0 <= msg.what) {
					mBtSppManager.disConnectDevice();
					readServerMessageStart(100);
					mReadServerMessageTimer.timerStart(100);
					mTextViewStatus.setText(MY_TURN);
					mServerMode = true;
				}
				return false;
			}
		});
		// サーバーがスレーブに接続したときの動作、メッセージボックス取得も動き出す

		mReadMessageTimer = new TimerHandler();
		mReadMessageTimer.setOnTickListener(new TimerHandler.OnTickListener() {
			@Override
			public void onTick() {
				if (isSocketExist()) {
					ArrayList<String> messages = getMessageMailBox();
					messageParse(messages.get(0));
				}
			}
		});
		// スレーブのメッセージボックスを定期的に取得する設定（まだスタートしない

		setOnConnectAction(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				if (0 <= msg.what) {
					mBtServerManager.stopBtServer();
					readMessageStart(100);
					mReadMessageTimer.timerStart(100);
					mTextViewStatus.setText(YOUR_TURN);
					mServerMode = false;
				}
				return false;
			}
		});
		// スレーブがサーバーに接続したときの動作、メッセージボックス取得も動き出す
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void messageParse(String message) {
		int index = message.indexOf(",");
		if (index != -1) {
			int row = Integer.parseInt(message.substring(0, index));
			int column = Integer.parseInt(message.substring(index + 1, message.length()));

			String tag = (String) mButtonCell[row][column].getTag();
			if (tag.equals("0P")) {
				mButtonCell[row][column].setBackgroundColor(Color.RED);
				mButtonCell[row][column].setTag("相手");
				mTextViewStatus.setText(MY_TURN);

				checkCell();
			}
		}
	}

	private void checkCell() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setPositiveButton("OK", null);

		boolean endFlag = true;

		int[] verticalCount = {0, 0, 0};
		String[] verticalTag = {
				(String) mButtonCell[0][0].getTag(),
				(String) mButtonCell[0][1].getTag(),
				(String) mButtonCell[0][2].getTag()
		};

		int[] diagonalCount = {0, 0};
		String[] diagonalTag = {
				(String) mButtonCell[0][0].getTag(),
				(String) mButtonCell[0][2].getTag()
		};

		for (int j = 0; j < 3; j++) {
			int horizontalCount = 0;
			String horizontalTag = (String) mButtonCell[j][0].getTag();
			for (int k = 0; k < 3; k++) {
				if (mButtonCell[j][k].getTag().equals("0P")) {
					endFlag = false;
				} else {
					if (mButtonCell[j][k].getTag().equals(horizontalTag)) {
						horizontalCount++;
					}

					if (mButtonCell[j][k].getTag().equals(verticalTag[k])) {
						verticalCount[k]++;
					}

					if (j == k) {
						if (mButtonCell[j][k].getTag().equals(diagonalTag[0])) {
							diagonalCount[0]++;
						}
					}

					if (k == 2 - j) {
						if (mButtonCell[j][k].getTag().equals(diagonalTag[1])) {
							diagonalCount[1]++;
						}
					}
				}
				if (horizontalCount == 3) {
					builder.setMessage(horizontalTag + "の勝ち！！");
					builder.show();
					return;
				} else if (verticalCount[k] == 3) {
					builder.setMessage(verticalTag[k] + "の勝ち！！");
					builder.show();
					return;
				} else if (diagonalCount[0] == 3) {
					builder.setMessage(diagonalTag[0] + "の勝ち！！");
					builder.show();
					return;
				} else if (diagonalCount[1] == 3) {
					builder.setMessage(diagonalTag[1] + "の勝ち！！");
					builder.show();
					return;
				}
			}
		}

		if (endFlag) {
			builder.setMessage("ひきわけ！！");
			builder.show();
			return;
		}

		// 勝ち負け判定
	}

	public void startServer(View v) {
		startBtServer("MarubatsuServer");
	}

	public void connectDevice(View v) {
		showDeviceSelectDialog();
	}

	public void restart(View v) {
		mBtSppManager.disConnectDevice();
		mBtServerManager.stopBtServer();

		Intent intent = new Intent(this, this.getClass());
		startActivity(intent);
		finish();
	}
}
