package com.example.mygcs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.command.ChangeSpeed;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.app.AlertDialog.*;
import static com.o3dr.services.android.lib.drone.property.VehicleMode.COPTER_GUIDED;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    NaverMap myMap;
    protected Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    Marker marker;
    private Spinner modeSelector;
    private double altitudeSetting = 0.0;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;

    public int Auto_Distance = 50;
    private int Auto_Marker_Count = 0;
    public int Gap_Distance = 5;
    private int Gap_Top = 0;
    public int Reached_Count = 0;

    private int markerCount1 = 0, markerCount2 = 0;
    private LatLng latLngAB;
    private ArrayList<Marker> markers;
    private Button mkVisibility_btn, mkInvisibility_btn;
    private TableLayout mkControl;
    private SwipeRefreshLayout refreshLayout = null;
    private WebView webView;


    protected double mRecentAltitude = 0;

    private boolean click1 = true;
    private boolean click2 = true;
    private boolean click3 = false;
    private boolean map_chg_click = true;
    private boolean cadastral_click = true;
    private boolean menu_click4 = true;
    private boolean ab_click = true;
    private boolean mission_click = true;

    ///////////////////////////////// 소켓 변수//////////////////////////////////
    private Handler mHandler;
    private Socket socket;
    private String ip = "192.168.0.24"; // IP 주소 192.168.43.235
    private int port = 9906; // PORT번호

    private DataOutputStream dos;
    private DataInputStream dis;

    private String msg;
    private byte[] data;
    private ByteBuffer b;
    int sock_msg1=0, sock_msg2=0, sock_msg3=0;
    private int count=0;
    ////////////////////////////////////////////////////////////////////////////

    List<Marker> Auto_Marker = new ArrayList<>();       // 간격감시 마커
    List<LatLng> PolygonLatLng = new ArrayList<>();     // 간격감시 폴리곤
    List<LatLng> Auto_Polyline = new ArrayList<>();     // 간격감시 폴리라인

    PolylineOverlay polyline = new PolylineOverlay();           // 마커 지나간 길
    PolygonOverlay polygon = new PolygonOverlay();              // 간격 감시 시 뒤 사각형 (하늘)
    PolylineOverlay polylinePath = new PolylineOverlay();
    PolygonOverlay Area_polygon = new PolygonOverlay();

    GuideMode guideMode;
    private UiSettings uiSettings;

    public MainActivity() {
        marker = new Marker();
    }

    // ################################### 미션 수행 Mission ######################################

    private void MakeWayPoint() {
        final Mission mMission = new Mission();

        //좌표설정
        for (int i = 0; i < Auto_Polyline.size(); i++) {
            Waypoint waypoint = new Waypoint();
            waypoint.setDelay(1);

            LatLongAlt latLongAlt = new LatLongAlt(Auto_Polyline.get(i).latitude, Auto_Polyline.get(i).longitude, mRecentAltitude);
            waypoint.setCoordinate(latLongAlt);

            mMission.addMissionItem(waypoint);
        }

        //미션 버튼 클릭시 이벤트
        final Button BtnSendMission = (Button) findViewById(R.id.sendMissionBtn);

        BtnSendMission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BtnSendMission.getText().equals("임무전송")) {
                      //connetSocket(BtnSendMission);
                    if (PolygonLatLng.size() == 4) {
                        setMission(mMission);
                    } else {
                        alertUser("A,B좌표 필요");
                    }
                } else if (BtnSendMission.getText().equals("임무시작")) {
                    InsertData task = new InsertData();
                    task.execute("http://192.168.0.24/android_connect.php", "YANG");
                    // Auto모드로 전환
                    ChangeToAutoMode();
                    BtnSendMission.setText("임무 수행 중");
                }
            }
        });
    }

    //미션 설정하기
    private void setMission(Mission mMission) {
        MissionApi.getApi(this.drone).setMission(mMission, true);
    }

    //미션중지
    private void pauseMission() {
        MissionApi.getApi(this.drone).pauseMission(null);
    }

    //미션 전송하기
    private void Mission_Sent() {
        alertUser("미션 업로드 완료");
        Button BtnSendMission = (Button) findViewById(R.id.sendMissionBtn);
        BtnSendMission.setText("임무 시작");
    }

    // ################################## 비행 모드 변경 ##########################################
    private void ChangeToLoiterMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Loiter 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Loiter 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패");
            }
        });
    }

    private void ChangeToAutoMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드 변경 실패.");
            }
        });
    }

    private void MakeGapPolygon(LatLng latLng) {
        if (Gap_Top < 2) {
            Marker marker = new Marker();
            marker.setPosition(latLng);
            PolygonLatLng.add(latLng);

            // Auto_Marker에 넣기 위해 marker 생성..
            Auto_Marker.add(marker);
            Auto_Marker.get(Auto_Marker_Count).setMap(myMap);

            latLngAB = Auto_Marker.get(0).getPosition();
            Marker markerAB = new Marker();

            if (Gap_Top == 0) {
                Auto_Marker.get(0).setIcon(OverlayImage.fromResource(R.drawable.a_point));
                Auto_Marker.get(0).setWidth(80);
                Auto_Marker.get(0).setHeight(80);
                Auto_Marker.get(0).setAnchor(new PointF(0.5F, 0.5F));

                markerCount1++;
                markerAB.setPosition(latLngAB);
                markerAB.setCaptionText(markerCount1 + "_지점");
                markers.add(markerAB);

                if (markerCount2 == 0) {
                    for (Marker marker_AB : markers) {
                        marker_AB.setMap(myMap);
                    }
                } else {
                    for (Marker marker_AB : markers.subList(markerCount2, markers.size())) {
                        alertUser("count2 :" + markerCount2);
                        marker_AB.setMap(myMap);
                    }
                }
            } else if (Gap_Top == 1) {
                Auto_Marker.get(1).setIcon(OverlayImage.fromResource(R.drawable.b_point));
                Auto_Marker.get(1).setWidth(80);
                Auto_Marker.get(1).setHeight(80);
                Auto_Marker.get(1).setAnchor(new PointF(0.5F, 0.5F));
            }

            Gap_Top++;
            Auto_Marker_Count++;
        }

        if (Auto_Marker_Count == 2) {
            double heading = MyUtil.computeHeading(Auto_Marker.get(0).getPosition(), Auto_Marker.get(1).getPosition());

            LatLng latLng1 = MyUtil.computeOffset(Auto_Marker.get(1).getPosition(), Auto_Distance, heading + 90);
            LatLng latLng2 = MyUtil.computeOffset(Auto_Marker.get(0).getPosition(), Auto_Distance, heading + 90);

            PolygonLatLng.add(latLng1);
            PolygonLatLng.add(latLng2);
            polygon.setCoords(Arrays.asList(
                    new LatLng(PolygonLatLng.get(0).latitude, PolygonLatLng.get(0).longitude),
                    new LatLng(PolygonLatLng.get(1).latitude, PolygonLatLng.get(1).longitude),
                    new LatLng(PolygonLatLng.get(2).latitude, PolygonLatLng.get(2).longitude),
                    new LatLng(PolygonLatLng.get(3).latitude, PolygonLatLng.get(3).longitude)));

            Log.d("Position5", "LatLng[0] : " + PolygonLatLng.get(0).latitude + " / " + PolygonLatLng.get(0).longitude);
            Log.d("Position5", "LatLng[1] : " + PolygonLatLng.get(1).latitude + " / " + PolygonLatLng.get(1).longitude);
            Log.d("Position5", "LatLng[2] : " + PolygonLatLng.get(2).latitude + " / " + PolygonLatLng.get(2).longitude);
            Log.d("Position5", "LatLng[3] : " + PolygonLatLng.get(3).latitude + " / " + PolygonLatLng.get(3).longitude);

            int colorLightBlue = getResources().getColor(R.color.colorLightBlue);

            polygon.setColor(colorLightBlue);
            polygon.setMap(myMap);

            // 내부 길 생성
            MakeGapPath();
        }

    }

    //미션 : 길 간격 계산
    private void MakeGapPath() {
        double heading = MyUtil.computeHeading(Auto_Marker.get(0).getPosition(), Auto_Marker.get(1).getPosition());

        Auto_Polyline.add(new LatLng(Auto_Marker.get(0).getPosition().latitude, Auto_Marker.get(0).getPosition().longitude));
        Auto_Polyline.add(new LatLng(Auto_Marker.get(1).getPosition().latitude, Auto_Marker.get(1).getPosition().longitude));

        for (int sum = Gap_Distance; sum + Gap_Distance <= Auto_Distance + Gap_Distance; sum = sum + Gap_Distance) {
            LatLng latLng1 = MyUtil.computeOffset(Auto_Marker.get(Auto_Marker_Count - 1).getPosition(), Gap_Distance, heading + 90);
            LatLng latLng2 = MyUtil.computeOffset(Auto_Marker.get(Auto_Marker_Count - 2).getPosition(), Gap_Distance, heading + 90);

            Auto_Marker.add(new Marker(latLng1));
            Auto_Marker.add(new Marker(latLng2));
            Auto_Marker_Count += 2;

            Auto_Polyline.add(new LatLng(Auto_Marker.get(Auto_Marker_Count - 2).getPosition().latitude, Auto_Marker.get(Auto_Marker_Count - 2).getPosition().longitude));
            Auto_Polyline.add(new LatLng(Auto_Marker.get(Auto_Marker_Count - 1).getPosition().latitude, Auto_Marker.get(Auto_Marker_Count - 1).getPosition().longitude));
        }

        polylinePath.setColor(Color.WHITE);
        polylinePath.setCoords(Auto_Polyline);
        polylinePath.setMap(myMap);

        // WayPoint
        MakeWayPoint();
    }

    // ################################## 간격 감시 시 Dialog #####################################

    //길 전체길이 설정 알림창
    private void DialogGap() {
        final EditText edittext1 = new EditText(this);

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("[간격 감시]");
        builder.setMessage("1. 전체 길이를 입력하십시오.");
        builder.setView(edittext1);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String editTextValue = edittext1.getText().toString();
                        Auto_Distance = Integer.parseInt(editTextValue);
                        DialogGap2();
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    //길 간격 길이 입력 알림창
    private void DialogGap2() {
        final EditText edittext2 = new EditText(this);

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("[간격 감시]");
        builder.setMessage("2. 간격 길이를 입력하십시오");
        builder.setView(edittext2);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String editTextValue = edittext2.getText().toString();
                        Gap_Distance = Integer.parseInt(editTextValue);
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }


    //지도 UI삭제 설정
    public void deleteUiVisibility() {
        this.uiSettings = myMap.getUiSettings();
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setZoomControlEnabled(false);
    }

    //지도 타입 확인함수
    public void checkMapType() {
        final ImageButton satellite_btn = (ImageButton) findViewById(R.id.satellite_btn);
        final ImageButton terrain_btn = (ImageButton) findViewById(R.id.terrain_btn);
        final ImageButton hybrid_btn = (ImageButton) findViewById(R.id.hybrid_btn);
        if (click3 == false) {
            hybrid_btn.setImageResource(R.drawable.hybrid_active);
        } else if (click2 == false) {
            terrain_btn.setImageResource(R.drawable.terrain_active);
        } else if (click1 == false) {
            satellite_btn.setImageResource(R.drawable.satellite_active);
        }
    }

    //비행모드 선택함수
    public void onFlightModeSelected(View view) {
        //스피너로 선택한 비행모드 받아와서 설정
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    //이륙고도 설정 버튼 안보이게 하는 함수
    public void deleteAltitudeMenuVisibility() {
        View view = null;
        Button alt_set = (Button) findViewById(R.id.altitude_setting);  //이륙고도 버튼
        Button inc_alt = (Button) findViewById(R.id.increase_altitude); // 0.5M 고도 상승버튼
        Button dec_alt = (Button) findViewById(R.id.decrease_altitude); // 0.5M 고도 하강버튼
        alt_set.setVisibility(view.GONE);
        inc_alt.setVisibility(view.GONE);
        dec_alt.setVisibility(view.GONE);
    }

    //지도타입 안보이게하는 함수
    public void deleteMapTypeMenuVisibility() {
        View view = null;
        final ImageButton satellite_btn = (ImageButton) findViewById(R.id.satellite_btn);
        final ImageButton terrain_btn = (ImageButton) findViewById(R.id.terrain_btn);
        final ImageButton hybrid_btn = (ImageButton) findViewById(R.id.hybrid_btn);

        satellite_btn.setVisibility(view.GONE);
        terrain_btn.setVisibility(view.GONE);
        hybrid_btn.setVisibility(view.GONE);
    }

    //유저에게 메시지 보이게 하는 함수
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }

    //고도 업데이트 함수
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitude_number);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        mRecentAltitude = droneAltitude.getRelativeAltitude();
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    //속도 업데이트 함수
    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.velocity_number);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    //전력 업데이트 함수
    protected void updateBattery() {
        TextView altitudeTextView = (TextView) findViewById(R.id.voltage_number);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        altitudeTextView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");
    }

    //yqw값 업데이트 함수
    protected void updateYAW() {
        TextView attitudeTextView = (TextView) findViewById(R.id.YAW_number);
        Attitude droneAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        attitudeTextView.setText(String.format("%3.1f", droneAttitude.getYaw()) + "deg");

        float droneAngle = (float) droneAttitude.getYaw();
        if (droneAngle < 0) {
            droneAngle += 360;
        }
        marker.setAngle(droneAngle);

    }

    //위성개수 업데이트 함수
    protected void updateNumberOfSatellite() {
        TextView numberOfSatellitesTextView = (TextView) findViewById(R.id.satellite_number);
        Gps droneNumberOfSatellites = this.drone.getAttribute(AttributeType.GPS);
        Log.d("MYLOG", "위성 수 변화 : " + droneNumberOfSatellites.getSatellitesCount());
        numberOfSatellitesTextView.setText(String.format("%3d", droneNumberOfSatellites.getSatellitesCount()));
    }

    //gps개수 업데이트 함수
    protected void updateGPS() {
        Gps droneLocation = this.drone.getAttribute(AttributeType.GPS);
        marker.setPosition(new LatLng(droneLocation.getPosition().getLatitude(), droneLocation.getPosition().getLongitude()));
        marker.setMap(myMap);
        marker.setIcon(OverlayImage.fromResource(R.drawable.gcsmarker));
        marker.setAnchor(new PointF((float) 0.5, (float) 0.77));
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(droneLocation.getPosition().getLatitude(), droneLocation.getPosition().getLongitude()));
        myMap.moveCamera(cameraUpdate);

        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());

    }

    //비행모드 업데이트 함수
    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    //모터가동버튼 업데이트 함수
    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.flight_btn);


        if (vehicleState.isFlying() && this.drone.isConnected()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }

    }

    //모터 가동시 경고 알림창 보여주는 함수
    public void showArmDialogue() {
        Builder alert = new Builder(this);
        alert.setTitle("경고");
        alert.setMessage("모터를 가동합니다.\n모터가 고속으로 회전합니다.");

        alert.setPositiveButton("확인", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                    @Override
                    public void onError(int executionError) {
                        alertUser("Unable to arm vehicle.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Arming operation timed out.");
                    }
                });
            }
        });

        alert.setNegativeButton("취소", new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });
        alert.show();
    }

    //기체 이륙 버튼 경고 알림창 보여주는 함수
    public void showTakeOffDialogue() {
        Builder alert = new Builder(this);
        alert.setTitle("경고");
        alert.setMessage("지정한 이륙 고도까지 기체가 상승합니다.\n안전거리를 유지하세요.");

        alert.setPositiveButton("확인", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ControlApi.getApi(drone).takeoff(altitudeSetting, new AbstractCommandListener() {

                    @Override
                    public void onSuccess() {
                        alertUser("Taking off...");
                    }

                    @Override
                    public void onError(int i) {
                        alertUser("Unable to take off.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Unable to take off.");
                    }
                });
            }
        });

        alert.setNegativeButton("취소", new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        alert.show();
    }

    protected void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            connectDrone();
        }
    }

    //비행모드타입 업데이트 함수
    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    //드론 연결 함수
    protected void connectDrone() {
        ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
        this.drone.connect(connectionParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //스크린 가로로 설정
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        markers = new ArrayList<>();
        mkControl = (TableLayout) findViewById(R.id.mkControl);
        mkInvisibility_btn = (Button) findViewById(R.id.mkInvisibility);
        mkVisibility_btn = (Button) findViewById(R.id.mkVisibility);
        webView = (WebView) findViewById(R.id.webView);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);
        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);

        //비행모드 아이템 선택 리스너
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                ((TextView) modeSelector.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        //맵 초기화 설정하는 asyncTask
        mapFragment.getMapAsync(this);

        this.guideMode = new GuideMode();

        //웹뷰 설정
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClientClass());
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl("http://192.168.0.24/android_connect.php");

        //웹뷰 reload함수
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
                alertUser("WebPage loading...");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                myMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {                 //뒤로가기 버튼 이벤트
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {    //웹뷰에서 뒤로가기 버튼을 누르면 뒤로가짐
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class WebViewClientClass extends WebViewClient {                //페이지 이동
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            refreshLayout.setRefreshing(false);
            alertUser("WebPage loading finish");

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("check URL", url);
            view.loadUrl(url);
            return true;
        }
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;
        myMap.setLocationSource(locationSource);
        myMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        deleteUiVisibility();
        deleteMapTypeMenuVisibility();
        deleteAltitudeMenuVisibility();

        marker.setIcon(OverlayImage.fromResource(R.drawable.gcsmarker));
        final ImageButton satellite_btn = (ImageButton) findViewById(R.id.satellite_btn);
        final ImageButton terrain_btn = (ImageButton) findViewById(R.id.terrain_btn);
        final ImageButton hybrid_btn = (ImageButton) findViewById(R.id.hybrid_btn);
        final ImageButton map_chg_btn = (ImageButton) findViewById(R.id.map_chg_btn);

        final ImageButton cadastral_btn = (ImageButton) findViewById(R.id.cadastral_btn);

        final Button clear_btn = (Button) findViewById(R.id.clear);
        final Button flight_btn = (Button) findViewById(R.id.flight_btn);
        final ImageButton connBtn = (ImageButton) findViewById(R.id.connect_btn);
        final Button altBtn = (Button) findViewById(R.id.altitude_setting);
        final Button inAltBtn = (Button) findViewById(R.id.increase_altitude);
        final Button deAltBtn = (Button) findViewById(R.id.decrease_altitude);

        final ImageButton mission_btn = (ImageButton) findViewById(R.id.mission_btn);
        final ImageButton ab_btn = (ImageButton) findViewById(R.id.ab_btn);
        final Button sendMissionBtn = (Button) findViewById(R.id.sendMissionBtn);
        final ImageButton menu_btn = (ImageButton) findViewById(R.id.menu_btn);
       // final Button socket_test_btn = (Button) findViewById(R.id.test);


        altBtn.setVisibility(View.VISIBLE);
        ab_btn.setVisibility(View.INVISIBLE);
        connBtn.setVisibility(View.INVISIBLE);

        myMap.setMapType(NaverMap.MapType.Hybrid);
        click3 = false;
        checkMapType();

        //connectSocket(socket_test_btn);
        /*socket_test_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket_test_btn.getText().equals("임무전송")) {
                        socket_test_btn.setText("임무시작");
                } else if (socket_test_btn.getText().equals("임무시작")) {
                    socket_test_btn.setText("임무중지");
                    count++;
                } else if (socket_test_btn.getText().equals("임무중지")) {
                    socket_test_btn.setText("임무재시작");
                    if(count==3){
                        socket_test_btn.setText("임무전송");
                    }
                } else if (socket_test_btn.getText().equals("임무재시작")) {
                    socket_test_btn.setText("임무중지");
                }
            }
        });*/


        menu_btn.setOnClickListener(new View.OnClickListener() {
            boolean click = true;
            @Override
            public void onClick(View view) {
                if (click) {
                    connBtn.setVisibility(View.VISIBLE);
                    click = false;
                } else {
                    connBtn.setVisibility(View.INVISIBLE);
                    click = true;
                }
            }
        });


        mission_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mission_click) {
                    mission_btn.setImageResource(R.drawable.mission_on);
                    mission_click = false;
                    ab_btn.setVisibility(View.VISIBLE);
                } else {
                    mission_btn.setImageResource(R.drawable.mission_btn);
                    mission_click = true;
                    ab_btn.setVisibility(View.INVISIBLE);
                }
            }
        });
        connBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnConnectTap(view);
            }
        });
        ab_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ab_click) {
                    ab_btn.setImageResource(R.drawable.ab_on);
                    ab_click = false;

                    sendMissionBtn.setVisibility(View.VISIBLE);

                    alertUser("A와 B좌표를 클릭하세요.");

                    DialogGap();

                    mission_btn.setImageResource(R.drawable.mission_btn);
                    mission_click = true;
                    ab_btn.setVisibility(View.INVISIBLE);

                } else {
                    ab_btn.setImageResource(R.drawable.ab_btn);
                    ab_click = true;
                    sendMissionBtn.setText("임무전송");
                    sendMissionBtn.setVisibility(View.INVISIBLE);
                }
            }
        });

        terrain_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (click2) {
                    if (click1 == false) {
                        satellite_btn.setImageResource(R.drawable.satellite_btn);
                    }
                    terrain_btn.setImageResource(R.drawable.terrain_active);
                    if (click3 == false) {
                        hybrid_btn.setImageResource(R.drawable.hybrid_btn);
                    }
                    click1 = true;
                    click2 = false;
                    click3 = true;
                    myMap.setMapType(NaverMap.MapType.Terrain);
                }
            }
        });


        hybrid_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (click3) {
                    if (click1 == false) {
                        satellite_btn.setImageResource(R.drawable.satellite_btn);
                    }
                    if (click2 == false) {
                        terrain_btn.setImageResource(R.drawable.terrain_btn);
                    }
                    hybrid_btn.setImageResource(R.drawable.hybrid_active);
                    click1 = true;
                    click2 = true;
                    click3 = false;
                    myMap.setMapType(NaverMap.MapType.Hybrid);
                }
            }
        });

        map_chg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (map_chg_click) {
                    map_chg_btn.setImageResource(R.drawable.map_chg_on);
                    map_chg_click = false;
                } else {
                    map_chg_btn.setImageResource(R.drawable.map_chg_btn);
                    map_chg_click = true;
                }
                if (satellite_btn.getVisibility() == view.GONE) {
                    satellite_btn.setVisibility(view.VISIBLE);
                } else {
                    satellite_btn.setVisibility(view.GONE);
                }
                if (terrain_btn.getVisibility() == view.GONE) {
                    terrain_btn.setVisibility(view.VISIBLE);
                } else {
                    terrain_btn.setVisibility(view.GONE);
                }
                if (hybrid_btn.getVisibility() == view.GONE) {
                    hybrid_btn.setVisibility(view.VISIBLE);
                } else {
                    hybrid_btn.setVisibility(view.GONE);
                }
            }
        });


        cadastral_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cadastral_click) {
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    cadastral_btn.setImageResource(R.drawable.cadastral_on);
                    cadastral_click = false;
                } else {
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    cadastral_btn.setImageResource(R.drawable.cadastral_off);
                    cadastral_click = true;
                }
            }
        });

        flight_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                State vehicleState = drone.getAttribute(AttributeType.STATE);

                if (vehicleState.isFlying()) {
                    // Land
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to land the vehicle.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Unable to land the vehicle.");
                        }
                    });
                } else if (vehicleState.isArmed()) {
                    // Take off
                    showTakeOffDialogue();
                } else if (!vehicleState.isConnected()) {
                    // Connect
                    alertUser("Connect to a drone first");
                } else {
                    // Connected but not Armed

                    showArmDialogue();
                }
            }
        });

        altBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inAltBtn.getVisibility() == view.GONE) {
                    inAltBtn.setVisibility(view.VISIBLE);
                } else {
                    inAltBtn.setVisibility(view.GONE);
                }
                if (deAltBtn.getVisibility() == view.GONE) {
                    deAltBtn.setVisibility(view.VISIBLE);
                } else {
                    deAltBtn.setVisibility(view.GONE);
                }
                if (menu_click4) {
                    altBtn.setBackgroundColor(Color.YELLOW);
                    menu_click4 = false;
                } else {
                    altBtn.setBackgroundColor(Color.parseColor("#d3d3d3"));
                    menu_click4 = true;
                }
            }
        });

        inAltBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                altitudeSetting += 0.5;
                altBtn.setText(String.format("이륙고도\n%3.1f", altitudeSetting) + "m");
            }
        });

        deAltBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (altitudeSetting >= 0.5) {
                    altitudeSetting -= 0.5;
                    altBtn.setText(String.format("이륙고도\n%3.1f", altitudeSetting) + "m");
                }
            }
        });

        mkVisibility_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mkControl.setVisibility(View.INVISIBLE);
                alertUser(mkVisibility_btn.getText().toString());
                for (Marker marker_AB : markers) {
                    marker_AB.setMap(myMap);
                }
            }
        });

        mkInvisibility_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mkControl.setVisibility(View.INVISIBLE);
                alertUser(mkInvisibility_btn.getText().toString());
                markerCount2 = markerCount1;
                for (Marker marker_AB : markers) {
                    marker_AB.setMap(null);
                }
            }
        });

        clear_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mkControl.getVisibility() == View.INVISIBLE)
                    mkControl.setVisibility(View.VISIBLE);
                else mkControl.setVisibility(View.INVISIBLE);

                // 폴리라인 / 폴리곤 지우기
                polyline.setMap(null);
                polygon.setMap(null);
                polylinePath.setMap(null);
                Area_polygon.setMap(null);

                // Auto_Marker 지우기
                if (Auto_Marker.size() != 0) {
                    for (int i = 0; i < Auto_Marker.size(); i++) {
                        Auto_Marker.get(i).setMap(null);
                    }
                }


                // 리스트 값 지우기
                Auto_Marker.clear();
                Auto_Polyline.clear();
                PolygonLatLng.clear();

                // Top 변수 초기화
                Auto_Marker_Count = 0;
                Gap_Top = 0;

                Reached_Count = 0;
                sock_msg3=0;
                sendMissionBtn.setText("임무 전송");

            }
        });

        myMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                State vehicleState = drone.getAttribute(AttributeType.STATE);
                if (ab_click == false) {
                    MakeGapPolygon(latLng);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }


    @Override
    public void onDroneEvent(String event, Bundle extras) {
        final Button sendMissionBtn = (Button) findViewById(R.id.sendMissionBtn);
        final ImageButton ab_btn = (ImageButton) findViewById(R.id.ab_btn);
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;
            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateYAW();
                break;
            case AttributeEvent.GPS_COUNT:
                updateNumberOfSatellite();
                break;
            case AttributeEvent.GPS_POSITION:
                updateGPS();
                break;
            case AttributeEvent.MISSION_SENT:
                Mission_Sent();
                break;
            case AttributeEvent.MISSION_ITEM_REACHED:
                alertUser(Reached_Count + "번 waypoint 도착");
                Reached_Count++;
                if(Reached_Count == Auto_Polyline.size()){
                    sendMissionBtn.setText("임무끝");
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch (connectionStatus.getStatusCode()) {
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }


    //가이드 모드
    class GuideMode {
        LatLng mGuidedPoint; //가이드모드 목적지 저장
        Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker(); //GCS 위치 표시 마커 옵션
        OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.gcsmarker);

        void DialogSimple(final Drone drone, final LatLong point) {
            State vehicleState = drone.getAttribute(AttributeType.STATE);
            VehicleMode vehicleMode = vehicleState.getVehicleMode();

            //this.mGuidedPoint = new LatLng(point.getLatitude(), point.getLongitude());

            if (vehicleMode != VehicleMode.COPTER_GUIDED) {
                AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
                alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Action for 'Yes' Button
                        VehicleApi.getApi(drone).setVehicleMode(COPTER_GUIDED,
                                new AbstractCommandListener() {
                                    @Override

                                    public void onSuccess() {
                                        ControlApi.getApi(drone).goTo(point, true, null);
                                    }

                                    @Override
                                    public void onError(int i) {

                                    }

                                    @Override
                                    public void onTimeout() {
                                    }
                                });
                    }
                }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

                AlertDialog alert = alt_bld.create();
                // Title for AlertDialog
                alert.setTitle("Title");
                // Icon for AlertDialog
                alert.setIcon(R.drawable.drone);
                alert.show();
            } else if (vehicleMode == VehicleMode.COPTER_GUIDED) {
                //this.mGuidedPoint = new LatLng(point.getLatitude(), point.getLongitude());
                ControlApi.getApi(drone).goTo(point, true, null);
            }
        }
    }


    //소켓
    private void connectSocket(Button btn){
        mHandler = new Handler();

        Log.w("connect","연결 하는중");
        // 받아오는거
        Thread checkUpdate = new Thread() {
            public void run() {
                // 서버 접속

                try {
                    socket = new Socket(ip, port);
                    Log.w("서버 접속됨", "서버 접속됨");

                } catch(NoRouteToHostException e){
                    e.printStackTrace();
                } catch (IOException e1) {
                    Log.w("서버접속못함", "서버접속못함");
                    e1.printStackTrace();
                }

                Log.w("edit 넘어가야 할 값 : ","안드로이드에서 서버로 연결요청");

                try {
                    dos = new DataOutputStream(socket.getOutputStream());   // output에 보낼 steram
                    dis = new DataInputStream(socket.getInputStream());     // 안드로이드가 받을 stream
                    // input에 받을꺼 넣어짐
                    dos.writeUTF("안드로이드에서 서버로 연결요청");   //서버에 전달
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.w("버퍼", "버퍼생성 잘못됨");
                }
                Log.w("버퍼","버퍼생성 잘됨");


                // 서버로 문자열 전송
                while(true) {
                    try {
                        //while문 안이므로 count체크 변수를 만들어서 서버에 한번만 출력되도록 설정
                        if (btn.getText().equals("임무수행중")) {
                            if(sock_msg1==0){
                                msg = "1";
                                data = msg.getBytes();
                                b = ByteBuffer.allocate(10);
                                b.order(ByteOrder.LITTLE_ENDIAN);

                                b.putInt(data.length);
                                dos.write(b.array(), 0, 10);
                                dos.write(data);
                                dos.flush();
                                sock_msg1 ++;
                                sock_msg2=0;
                                readSocketMSG();
                            }
                        } else if (btn.getText().equals("임무끝") ) {
                            if(sock_msg2==0){
                                msg = "2";
                                data = msg.getBytes();
                                b = ByteBuffer.allocate(10);
                                b.order(ByteOrder.LITTLE_ENDIAN);
                                b.putInt(data.length);
                                dos.write(b.array(), 0, 10);
                                dos.write(data);
                                dos.flush();
                                sock_msg2++;
                                sock_msg1=0;
                                readSocketMSG();
                            }
                        } else if (btn.getText().equals("임무시작")) {
                            if(sock_msg3==0){
                                sock_msg1=0;
                                sock_msg2=0;
                                sock_msg3++;
                                msg = "3";
                                data = msg.getBytes();
                                b = ByteBuffer.allocate(10);
                                b.order(ByteOrder.LITTLE_ENDIAN);
                                b.putInt(data.length);
                                dos.write(b.array(), 0, 10);
                                dos.write(data);
                                dos.flush();
                                readSocketMSG();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }
        };
        // 소켓 접속 시도, 버퍼생성
        checkUpdate.start();
    }

    //서버에서 받은 문자열 읽는 함수
    private void readSocketMSG(){
        try {
            data = new byte[10];
            dis.read(data, 0, 10);
            ByteBuffer b = ByteBuffer.wrap(data);
            b.order(ByteOrder.LITTLE_ENDIAN);
            int length = b.getInt();
            data = new byte[length];
            dis.read(data,0,length);
            msg = new String(data, "UTF-8");
            System.out.println("-----서버에서 온 값 : "+msg);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("대기중");
        }
    }



    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = (String) params[0];


            try {
                String name = (String) params[1];
                String postParameters = "name=" + name;

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(3000);
                httpURLConnection.setConnectTimeout(3000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d("POST_RESPONSE_2", "test : " + responseStatusCode);

                InputStream inputStream;

                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                    Log.d("CONN", "성공");
                }
                else {
                    inputStream = httpURLConnection.getErrorStream();
                    Log.d("CONN","실패");
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line=bufferedReader.readLine()) != null){
                    sb.append(line);
                }
                bufferedReader.close();
                Log.d("Check", "Insert"+sb.toString().trim());
//
                return null;

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ExceptionError","InsertData : Error "+e);
                alertUser("연결안됨"+e.getMessage());
                return null;
            }

        }
    }


}
