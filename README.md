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
  ● 安装Android Studio（此项目使用2022.3.1版本）  

步骤二、  
  ● https://mirrors.aliyun.com/github/releases/gradle/gradle-distributions/v6.7.1/  
  ● 下载gradle6.7.1，并放在适当的位置    

步骤三、  
  ● 克隆此项目，使用Android Studio打开   
  ● 打开设置，按照下面这样选择gradle和JDK，可以减少出错概率  
  <img width="838" height="319" alt="c896c25182862b6acf34452e49f816d1" src="https://github.com/user-attachments/assets/253af3ae-0251-46f1-948b-e608366f5d59" />  
  
步骤四、  
  ● 注册虹软人脸识别adk
  ● 新建一个免费的人脸识别应用，将对应的APP_ID与SDK_KEY填入项目代码:  
      <img width="2517" height="572" alt="3333b90de854d9a8cf7f0b9bdb5e1120" src="https://github.com/user-attachments/assets/50cf622f-1bbd-481b-a867-5c6eb601786a" />  
      <img width="1964" height="927" alt="0de10c605e993e6d2a81a60d3d42518e" src="https://github.com/user-attachments/assets/f9937bfe-1d3b-4e49-b4cf-82254472040f" />  



运行效果:  
<video src="https://github.com/user-attachments/assets/76baf2dc-dbb9-453a-b230-7379bdd9776b" controls width="800">
  Your browser does not support the video tag.
</video>  














yolo11-pose模型转换指南   
---
步骤一、  
https://github.com/ultralytics/ultralytics  
● 克隆yolo11官方项目  

步骤二、  
● 下载默认权重或使用自己训练的权重文件  
● 在自己的环境里运行: yolo export model=yolo11n-pose.pt format=ncnn  
● 在同级目录下的yolo11n-pose_ncnn_model文件里可以找到:model.ncnn.bin、model.ncnn.param  
