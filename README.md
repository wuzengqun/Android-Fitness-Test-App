ncnn-android-yolo11-pose-app 
---
这是一个示例 ncnn android 项目，包含人脸识别和体测动作计数，它依赖于 ncnn 库、 opencv、虹软人脸识别ADK（免费的人脸识别只能用于android4.4-android10.0）:  
https://github.com/Tencent/ncnn  
https://github.com/nihui/opencv-mobile  
https://github.com/nihui/mesa-turnip-android-driver  
https://ai.arcsoft.com.cn/product/arcface.html   # 虹软


如何构建和运行
----
步骤一、  
&emsp;● 安装Android Studio（此项目使用2022.3.1版本）  

步骤二、  
&emsp;● https://mirrors.aliyun.com/github/releases/gradle/gradle-distributions/v6.7.1/  
&emsp;● 下载gradle6.7.1，并放在适当的位置    

步骤三、  
&emsp;● 克隆此项目，使用Android Studio打开   
&emsp;● 打开设置，按照下面这样选择gradle和JDK，可以减少出错概率  
<div align="center">
  <img width="838" height="319" alt="c896c25182862b6acf34452e49f816d1" src="https://github.com/user-attachments/assets/253af3ae-0251-46f1-948b-e608366f5d59" />  
<div align="center">
  
步骤四、  
&emsp;● 注册虹软人脸识别adk  
&emsp;● 新建一个免费的人脸识别应用，将对应的APP_ID与SDK_KEY填入项目代码:  
      <img width="2517" height="572" alt="3333b90de854d9a8cf7f0b9bdb5e1120" src="https://github.com/user-attachments/assets/50cf622f-1bbd-481b-a867-5c6eb601786a" />  
      <img width="1964" height="927" alt="0de10c605e993e6d2a81a60d3d42518e" src="https://github.com/user-attachments/assets/f9937bfe-1d3b-4e49-b4cf-82254472040f" />  

步骤五、  
&emsp;● 进入软件，注册后登录  
&emsp;● 进入我的，点击激活，即可开始人脸录入和体测计数  


运行效果（由于手头上没有低版本的android手机，这里只展示界面）:  
<div align="center">
<video src="https://github.com/user-attachments/assets/c651a750-5fc4-4393-bed3-e97c7c7c8080" controls width="800">
  Your browser does not support the video tag.
</video>
</div>

















yolo11-pose模型转换指南   
---
步骤一、  
&emsp; https://github.com/ultralytics/ultralytics  
&emsp;● 克隆yolo11官方项目  

步骤二、  
&emsp;● 下载默认权重或使用自己训练的权重文件  
&emsp;● 在自己的环境里运行: yolo export model=yolo11n-pose.pt format=ncnn  
&emsp;● 在同级目录下的yolo11n-pose_ncnn_model文件里可以找到:model.ncnn.bin、model.ncnn.param  
