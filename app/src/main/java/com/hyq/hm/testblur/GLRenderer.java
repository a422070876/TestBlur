package com.hyq.hm.testblur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by 海米 on 2017/8/16.
 */

public class GLRenderer {
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVertexBuffer;
    private int programId = -1;
    private int aPositionHandle;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int widthOfsetHandle;
    private int heightOfsetHandle;
    private int gaussianWeightsHandle;
    private int blurRadiusHandle;
    private int blurSigmaHandle;



    public GLRenderer(){
        final float[] vertexData = {
                1f, -1f, 0f,
                -1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
        };


        final float[] textureVertexData = {
                1f, 0f,
                0f, 0f,
                1f, 1f,
                0f, 1f
        };
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);
    }

    public void initShader(Context context){
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.vertext_shader);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.fragment_sharder);

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");

        widthOfsetHandle = GLES20.glGetUniformLocation(programId, "widthOfset");
        heightOfsetHandle = GLES20.glGetUniformLocation(programId, "heightOfset");
        gaussianWeightsHandle = GLES20.glGetUniformLocation(programId, "gaussianWeights");
        blurRadiusHandle = GLES20.glGetUniformLocation(programId, "blurRadius");
        blurSigmaHandle = GLES20.glGetUniformLocation(programId, "blurSigma");

        GLES20.glGenTextures(1, textures,0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }
    private  Rect rect = new Rect();
    private int textures[] =  new int[1];
    private int bitmapWidth,bitmapHeight;
    public void setBitmap(Bitmap bitmap){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);
        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();
    }

    public void setScreenSize(int screenWidth,int screenHeight){
        float sh = screenWidth * 1.0f / screenHeight;
        float vh = bitmapWidth * 1.0f / bitmapHeight;
        int left, top, viewWidth, viewHeight;
        if (sh < vh) {
            left = 0;
            viewWidth = screenWidth;
            viewHeight = (int) (bitmapHeight * 1.0f / bitmapWidth * viewWidth);
            top = (screenHeight - viewHeight) / 2;
        } else {
            top = 0;
            viewHeight = screenHeight;
            viewWidth = (int) (bitmapWidth * 1.0f / bitmapHeight * viewHeight);
            left = (screenWidth - viewWidth) / 2;
        }
        rect.left = left;
        rect.top = top;
        rect.right = left + viewWidth;
        rect.bottom = top + viewHeight;
    }

    private FloatBuffer gaussianWeightsBuffer;
    public void gaussianWeights(){
        if(blurRadius == 0 || sigma == 0){
            return;
        }
        float sumOfWeights = 0.0f;
        int g = 0;
        int tx = blurRadius*2+1;
        float gaussianWeights[] = new float[tx*tx];
        for (int x = -blurRadius; x <= blurRadius; x++) {
            for (int y = -blurRadius; y <= blurRadius; y++) {
                int s = x*x+y*y;
                float a = (float) ((1.0f / 2.0f * Math.PI * Math.pow(sigma, 2.0f)) * Math.exp(-s / (2.0f * Math.pow(sigma, 2.0f))));
                gaussianWeights[g] = a;
                sumOfWeights+=a;
                g++;
            }
        }
        for (int x = 0; x < tx*tx; ++x) {
            gaussianWeights[x] = gaussianWeights[x]/sumOfWeights;
        }
        gaussianWeightsBuffer = ByteBuffer.allocateDirect(gaussianWeights.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(gaussianWeights);
        gaussianWeightsBuffer.position(0);
    }
    private int blurRadius = 2;
    public void setBlurRadius(int blurRadius) {
        this.blurRadius = blurRadius;
    }

    private int sigma = 3;
    public void setSigma(int sigma) {
        this.sigma = sigma;
    }

    public void drawFrame(){
        GLES20.glViewport(rect.left, rect.top, rect.width(), rect.height());
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(programId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(uTextureSamplerHandle, 0);

        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);

        GLES20.glUniform1f(widthOfsetHandle, 1.0f/bitmapWidth);
        GLES20.glUniform1f(heightOfsetHandle, 1.0f/bitmapHeight);
        GLES20.glUniform1i(blurRadiusHandle, blurRadius);
        GLES20.glUniform1i(blurSigmaHandle, sigma);

        int tx = blurRadius*2+1;
        GLES20.glUniform1fv(gaussianWeightsHandle,tx*tx,gaussianWeightsBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
