/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surrey.ar.es00539arlocator;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.function.Consumer;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class ARLocatorActivity extends AppCompatActivity {
  private static final String TAG = ARLocatorActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private ArFragment arFragment;
  private ModelRenderable currentRenderable;
  private ModelRenderable keysRenderable;
  private AnchorNode keysNode;
  private ModelRenderable oculosRenderable;
  private AnchorNode oculosNode;
  private ImageButton modelButton;
  private ImageButton mapButton;
  private TextView locationView;
  private ImageView overmapView;

  protected void addModel(Consumer<ModelRenderable> im, int resource) {
      ModelRenderable.builder()
              .setSource(this, resource)
              .build()
              .thenAccept(im)
              .exceptionally(
                      throwable -> {
                          Toast toast =
                                  Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG);
                          toast.setGravity(Gravity.CENTER, 0, 0);
                          toast.show();
                          return null;
                      });
  }

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);

    addModel(renderable -> {
        oculosRenderable = renderable;
        currentRenderable = renderable;
    }, R.raw.oculos);
    addModel(renderable -> keysRenderable = renderable, R.raw.keys);

    overmapView = (ImageView)findViewById(R.id.overmapView);
    modelButton = (ImageButton)findViewById(R.id.modelButton);
    mapButton = (ImageButton)findViewById(R.id.mapButton);
    locationView = (TextView)findViewById(R.id.locationView);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    locationView.setText("Location: Kitchen");

    mapButton.setOnClickListener((View v) -> {
        if (overmapView.getVisibility() == View.VISIBLE) {
            overmapView.setVisibility(View.GONE);
        } else {
            overmapView.setVisibility(View.VISIBLE);
        }
    });

    modelButton.setOnClickListener((View v) -> {
        if (currentRenderable == oculosRenderable) {
            currentRenderable = keysRenderable;
            modelButton.setImageResource(R.drawable.preview_keys);
        } else if (currentRenderable == keysRenderable) {
            currentRenderable = oculosRenderable;
            modelButton.setImageResource(R.drawable.preview_glasses);
        }
    });

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (currentRenderable == null) {
            return;
          }

          if (currentRenderable == oculosRenderable && oculosNode != null) {
              oculosNode.getAnchor().detach();
              oculosNode.setParent(null);
              oculosNode = null;
          } else if (currentRenderable == keysRenderable && keysNode != null) {
              keysNode.getAnchor().detach();
              keysNode.setParent(null);
              keysNode = null;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable and add it to the anchor.
          TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
          node.setParent(anchorNode);
          node.setRenderable(currentRenderable);
          node.select();

          if (currentRenderable == oculosRenderable) {
              oculosNode = anchorNode;
          } else if (currentRenderable == keysRenderable) {
              keysNode = anchorNode;
          }
        });
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }
}
