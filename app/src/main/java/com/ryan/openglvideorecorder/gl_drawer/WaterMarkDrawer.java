package com.ryan.openglvideorecorder.gl_drawer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.ryan.openglvideorecorder.gl_utils.GLShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class WaterMarkDrawer {

    // 顶点着色器
    private static final String vertexSource
            = "uniform mat4 uMVPMatrix;\n"
            + "attribute vec4 aVertex;\n"
            + "attribute vec4 aColor;\n"
            + "varying vec4 vColor;\n"
            + "void main() {\n"
            + "	vColor = aColor;\n"
            + "	gl_Position = uMVPMatrix * aVertex;\n"
            + "}\n";

    // 片元着色器
    private static final String fragmentSource
            = "precision mediump float;\n"
            + "varying vec4 vColor;\n"
            + "void main() {\n"
            + "  gl_FragColor = vColor;\n"
            + "}";

    private int mProgram;
    private int mAttrVertex;
    private int mAttrColor;
    private int mUniMVPMatrix; // 总变换矩阵引用

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;


    static float[] mMMatrix = new float[] {
            1, 0 ,0 ,0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    private float[] vertex_array = new float[] {
            -0.85f, 0.95f, 0.0f,
            -0.95f, 0.85f, 0.0f,
            -0.75f, 0.85f, 0.0f
    };

    private float[] color_array = new float[] {
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    };

    // 每个顶点有3个坐标
    private final int COORDS_PER_VERTEX = 3;
    //顶点个数
    private final int vertexCount = vertex_array.length / COORDS_PER_VERTEX; // 3
    //顶点之间的偏移量
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节

    public WaterMarkDrawer() {
        initVertexData();
        initColorData();
        initShader();
    }

    private void initVertexData() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertex_array.length *4);
        buffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = buffer.asFloatBuffer();
        mVertexBuffer.put(vertex_array);
        mVertexBuffer.position(0);
    }

    private void initColorData() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(color_array.length *4);
        buffer.order(ByteOrder.nativeOrder());
        mColorBuffer = buffer.asFloatBuffer();
        mColorBuffer.put(color_array);
        mColorBuffer.position(0);
    }

    private void initShader() {
        mProgram = GLShaderUtil.createProgram(vertexSource, fragmentSource);
        mAttrVertex = GLES20.glGetAttribLocation(mProgram, "aVertex");
        mAttrColor = GLES20.glGetAttribLocation(mProgram, "aColor");
        mUniMVPMatrix = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    }


    /**
     * 绘制
     *
     */
    public void draw() {
        //指定使用某套shader程序
        GLES20.glUseProgram(mProgram);

        //初始化变换矩阵
//        Matrix.setRotateM(mMMatrix,0,45,0,0,1);
        //设置沿Z轴正向位移1
//        Matrix.translateM(mMMatrix,0,0,0,-1);
        //设置绕x轴旋转
//        Matrix.rotateM(mMMatrix,0, 40,0,0,1);
        //传入总的变化矩阵
        GLES20.glUniformMatrix4fv(mUniMVPMatrix, 1, false, mMMatrix , 0);

        //将顶点位置数据传送进渲染管线
        GLES20.glVertexAttribPointer(
                mAttrVertex, // 顶点坐标引用
                3, // 每个顶点有3个值x, y, z
                GLES20.GL_FLOAT, // 顶点类型
                false, // 是否需要归一化
                vertexStride, // 每个值占4个字节
                mVertexBuffer
        );
        //将顶点颜色数据传送进渲染管线
        GLES20.glVertexAttribPointer(
                mAttrColor, // 顶点颜色引用
                3, // 每个顶点有3个值，  G, B, A
                GLES20.GL_FLOAT,
                false,
                vertexStride, // 每个值占4个字节,  stride指定从一个属性到下一个属性的字节跨度，允许将顶点和属性打包到单个数组中或存储在单独的数组中
                mColorBuffer
        );
        GLES20.glEnableVertexAttribArray(mAttrVertex);//启用顶点位置数据
        GLES20.glEnableVertexAttribArray(mAttrColor);//启用顶点着色数据
        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount); // 有3个顶点，就是一个普通的三角形

        GLES20.glDisableVertexAttribArray(mAttrVertex);//启用顶点位置数据
        GLES20.glDisableVertexAttribArray(mAttrColor);//启用顶点着色数据
    }


//    public static float[] mProjMatrix = new float[16];//4x4 投影矩阵
//    public static float[] mVMatrix = new float[16];//摄像机位置朝向的参数矩阵
//    public static float[] mMVPMatrix;//最后起作用的总变换矩阵
//    public static float[] getFianlMatrix(float[] spec)
//    {
//        mMVPMatrix=new float[16];
//        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, spec, 0);
//        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
//        return mMVPMatrix;
//    }
}
