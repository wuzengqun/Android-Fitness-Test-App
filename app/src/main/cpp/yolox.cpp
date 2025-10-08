//
// Created by wangke on 2024/4/21.
//

#include "yolox.h"
#include <cpu.h>
#include <iostream>
#include <vector>

value_result v1;

const int MAX_STRIDE = 32;
const int COCO_POSE_POINT_NUM = 17;

const std::vector<std::vector<unsigned int>> KPS_COLORS =
        { {0,   255, 0}, {0,   255, 0},  {0,   255, 0}, {0,   255, 0},
          {0,   255, 0},  {255, 128, 0},  {255, 128, 0}, {255, 128, 0},
          {255, 128, 0},  {255, 128, 0},  {255, 128, 0}, {51,  153, 255},
          {51,  153, 255},{51,  153, 255},{51,  153, 255},{51,  153, 255},
          {51,  153, 255}};

const std::vector<std::vector<unsigned int>> SKELETON =
        { {16, 14},  {14, 12},  {17, 15},  {15, 13},   {12, 13}, {6,  12},
          {7,  13},  {6,  7},   {6,  8},   {7,  9},   {8,  10},  {9,  11},
          {2,  3}, {1,  2},  {1,  3},  {2,  4},  {3,  5},   {4,  6},  {5,  7} };

const std::vector<std::vector<unsigned int>> LIMB_COLORS =
        { {51,  153, 255}, {51,  153, 255},   {51,  153, 255},
          {51,  153, 255}, {255, 51,  255},   {255, 51,  255},
          {255, 51,  255}, {255, 128, 0},     {255, 128, 0},
          {255, 128, 0},   {255, 128, 0},     {255, 128, 0},
          {0,   255, 0},   {0,   255, 0},     {0,   255, 0},
          {0,   255, 0},   {0,   255, 0},     {0,   255, 0},
          {0,   255, 0} };

typedef struct {
    cv::Rect box;
    float confidence;
    int index;
}BBOX;

bool cmp_score(BBOX box1, BBOX box2) {
    return box1.confidence > box2.confidence;
}


static float get_iou_value(cv::Rect rect1, cv::Rect rect2)
{
    int xx1, yy1, xx2, yy2;

    xx1 = std::max(rect1.x, rect2.x);
    yy1 = std::max(rect1.y, rect2.y);
    xx2 = std::min(rect1.x + rect1.width - 1, rect2.x + rect2.width - 1);
    yy2 = std::min(rect1.y + rect1.height - 1, rect2.y + rect2.height - 1);

    int insection_width, insection_height;
    insection_width = std::max(0, xx2 - xx1 + 1);
    insection_height = std::max(0, yy2 - yy1 + 1);

    float insection_area, union_area, iou;
    insection_area = float(insection_width) * insection_height;
    union_area = float(rect1.width * rect1.height + rect2.width * rect2.height - insection_area);
    iou = insection_area / union_area;
    return iou;
}

void my_nms_boxes(std::vector<cv::Rect>& boxes, std::vector<float>& confidences, float confThreshold, float nmsThreshold, std::vector<int>& indices)
{
    BBOX bbox;
    std::vector<BBOX> bboxes;
    int i, j;
    for (i = 0; i < boxes.size(); i++)
    {
        bbox.box = boxes[i];
        bbox.confidence = confidences[i];
        bbox.index = i;
        bboxes.push_back(bbox);
    }
    sort(bboxes.begin(), bboxes.end(), cmp_score);

    int updated_size = bboxes.size();
    for (i = 0; i < updated_size; i++)
    {
        if (bboxes[i].confidence < confThreshold)
            continue;
        indices.push_back(bboxes[i].index);
        for (j = i + 1; j < updated_size; j++)
        {
            float iou = get_iou_value(bboxes[i].box, bboxes[j].box);
            if (iou > nmsThreshold)
            {
                bboxes.erase(bboxes.begin() + j);
                j=j-1;
                updated_size = bboxes.size();
            }
        }
    }
}

//函数：检查俯卧撑是否标准，下面多个都是
//计算两点斜率
static float calculateSlope(float x1, float y1, float x2, float y2) {
    if (x2 == x1) {
        // 如果x1和x2相等，则斜率不存在（垂直线）
        //throw std::invalid_argument("Undefined slope: points have the same x-coordinate.");
        return -1;
    }
    return (y2 - y1) / (x2 - x1);
}

//计算两点角度
static int calculateangle(int x1, int y1, int x2, int y2) {
    // 计算两点之间的差值
    double dx = static_cast<double>(x2 - x1); // 使用 double 保持精度
    double dy = static_cast<double>(y2 - y1);

    // 使用atan2计算角度，结果为弧度
    double angle_radians = atan2(dy, dx);

    // 将弧度转换为度
    double angle_degrees = angle_radians * (180.0 / M_PI);

    // 确保角度为正值
    if (angle_degrees < 0) {
        angle_degrees += 360.0;
    }

    // 将结果转换为 int
    return static_cast<int>(angle_degrees + 0.5); // 四舍五入
}

//计算两点距离
static double distance(int x1, int y1, int x2, int y2)
{
    return (std::sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
}

//计算两向量的点积
static double dot_product(int x1, int y1, int x2, int y2, int x3, int y3) {
    return ((x1 - x2) * (x3 - x2) + (y1 - y2) * (y3 - y2));
}

//计算两条直线的夹角（以度为单位）
static int calculate_angle(int x1, int y1, int x2, int y2, int x3, int y3) {
    // 构建向量 BA 和 BC
    double BAx = x1 - x2;
    double BAy = y1 - y2;
    double BCx = x3 - x2;
    double BCy = y3 - y2;

    // 计算点积
    double dot = dot_product(x1, y1, x2, y2, x3, y3);

    // 计算向量的模
    double magnitude_BA = distance(x1, y1, x2, y2);
    double magnitude_BC = distance(x3, y3, x2, y2);

    // 计算夹角的余弦值
    double cos_theta = dot / (magnitude_BA * magnitude_BC);

    // 确保余弦值在有效范围内
    cos_theta = std::max(-1.0, std::min(1.0, cos_theta));

    // 计算夹角（弧度）
    double theta_radians = std::acos(cos_theta);

    // 将弧度转换为度
    double theta_degrees = theta_radians * (180.0 / M_PI);

    return static_cast<int>(theta_degrees);
}

//判断身体是否大致平直
static bool is_body_straight(const float* preds,float error_threshold,int direction)
{
    if (direction == 1)//右边视角
    {
        float slope1 = calculateSlope(preds[16],preds[16+17],preds[14],preds[14+17]);
        float slope2 = calculateSlope(preds[14],preds[14+17],preds[12],preds[12+17]);
        float slope3 = calculateSlope(preds[12],preds[12+17],preds[6],preds[6+17]);
        return abs(slope1 - slope2) < error_threshold && abs(slope2 - slope3) < error_threshold;
    }
    else
    {
        float slope1 = calculateSlope(preds[15],preds[15+17],preds[13],preds[13+17]);
        float slope2 = calculateSlope(preds[13],preds[13+17],preds[11],preds[11+17]);
        float slope3 = calculateSlope(preds[11],preds[11+17],preds[5],preds[5+17]);
        return abs(slope1 - slope2) < error_threshold && abs(slope2 - slope3) < error_threshold;
    }
}

//函数：检查深蹲时角度是否合适
static bool is_situp_bodyangle(float angle1,float angle2,float angle3,float angle_threshold,int direction)
{
    if (direction == 1)//右边视角
    {
        return ((angle1 >= 95) && (angle1 <= 125)) && ((angle2 >= 175) && (angle2 <= 205)) && ((angle3 >= 112) && (angle3 <= 130));
    }
    else//左边视角，这里还没有实现，需要改一改角度范围
    {
        return ((angle1 >= 95) && (angle1 <= 125)) && ((angle2 >= 175) && (angle2 <= 205)) && ((angle3 >= 112) && (angle3 <= 130));
    }
}

//判断仰卧起坐是否标准
static bool is_crunch_standard(const float* preds,float height_threshold,int direction)
{
    static int tui_angle = 0;
    static int bei_angle = 0;
    static bool tui_angle_result = 0;
    static bool bei_angle_result = 0;
    static bool lastSitUpResult = false;
    static bool currentResult = false;
    if(direction == 1)//右边视角,头朝左
    {
        tui_angle = calculate_angle(preds[12],preds[12+17],preds[14],preds[14+17],preds[16],preds[16+17]);
        tui_angle_result = tui_angle >=70 && tui_angle <= 130;
        bei_angle = calculateangle(preds[6],preds[6+17],preds[12],preds[12+17]);
        bei_angle_result = bei_angle >= 80  && bei_angle <= 100;
        currentResult = tui_angle_result && bei_angle_result;
        if(lastSitUpResult == false && currentResult)
        {
            lastSitUpResult = true;
            return true;
        }
        else if(lastSitUpResult == true && currentResult)
        {
            return false;
        }
        else if((!currentResult) && ((bei_angle_result >= 0) && (bei_angle_result <= 20))) //背放平了才算做完一个
        {
            lastSitUpResult = false;
            return false;
        }
        else
        {
            return false;
        }
    }
    else if(direction == 0)//左边视角,头朝右
    {
        tui_angle = calculate_angle(preds[15],preds[15+17],preds[13],preds[13+17],preds[11],preds[11+17]);
        tui_angle_result = tui_angle >=70 && tui_angle <= 130;
        bei_angle = calculateangle(preds[5],preds[5+17],preds[11],preds[11+17]);
        bei_angle_result = bei_angle >= 80  && bei_angle <= 100;
        currentResult = tui_angle_result && bei_angle_result;
        if(lastSitUpResult == false && currentResult)
        {
            lastSitUpResult = true;
            return true;
        }
        else if(lastSitUpResult == true && currentResult)
        {
            return false;
        }
        else if((!currentResult) && (155 <= bei_angle_result)) //背放平了才算做完一个
        {
            lastSitUpResult = false;
            return false;
        }
        else
        {
            return false;
        }
    }

}

//函数：计算深蹲是否标准
static bool is_situp_standard(const float* preds,float height_threshold,int direction)
{
    static bool lastSitUpResult = false;
    static bool currentResult = false;
    if (direction == 1)//右边视角
    {
        float angle1 = calculateangle(preds[6],preds[6+17],preds[12],preds[12+17]);
        float angle2 = calculateangle(preds[14],preds[14+17],preds[12],preds[12+17]);
        float angle3 = calculateangle(preds[14],preds[14+17],preds[16],preds[16+17]);
        currentResult = is_situp_bodyangle(angle1,angle2,angle3,0,1);
        if(lastSitUpResult == false && currentResult)
        {
            lastSitUpResult = true;
            return true;
        }
        else if(lastSitUpResult == true && currentResult)
        {
            return false;
        }
        else if((!currentResult) && (angle1 < 95))  //背挺直了才算做完一个
        {
            lastSitUpResult = false;
            return false;
        }
        else
        {
            return false;
        }
    }
    if (direction == 0)//左边视角
    {
        float angle1 = calculateangle(preds[5],preds[5+17],preds[11],preds[11+17]);
        float angle2 = calculateangle(preds[13],preds[13+17],preds[11],preds[11+17]);
        float angle3 = calculateangle(preds[13],preds[13+17],preds[15],preds[15+17]);
        currentResult = is_situp_bodyangle(angle1,angle2,angle3,0,0);
        if(lastSitUpResult == false && currentResult)
        {
            lastSitUpResult = true;
            return true;
        }
        else if(lastSitUpResult == true && currentResult)
        {
            return false;
        }
        else if((!currentResult) && (angle1 < 95))  //背挺直了才算做完一个
        {
            lastSitUpResult = false;
            return false;
        }
        else
        {
            return false;
        }
    }

}

//判断俯卧撑是否标准（肩部与手肘关键点的位置）
//static bool is_pushup_standard(const float* preds,float height_threshold,int direction)
//{
//    static bool lastPullUpResult = false;
//    static float fucking = 0;
//    static float shoulder_x = 0;
//    static float elbow_x = 0;
//    static bool currentResult = false;
//    if (direction == 1)//右边视角
//    {
//        shoulder_x = preds[0 + 17];
//        elbow_x = preds[8 +17];
//        fucking = preds[8 +17] - preds[0 + 17];
//        currentResult = (shoulder_x + height_threshold) >= elbow_x;
//        if(lastPullUpResult == false && currentResult)
//        {
//            lastPullUpResult = true;
//            return true;
//        }
//        else if(lastPullUpResult == true && currentResult)
//        {
//            return false;
//        }
//        else if((!currentResult) && (fucking > 50))  //手伸直了才能算做完一个
//        {
//            lastPullUpResult = false;
//            return false;
//        }
//        else
//        {
//            return false;
//        }
//    }
//    else
//    {
//        shoulder_x = preds[0 + 17];
//        elbow_x = preds[7 +17];
//        fucking = preds[7 +17] - preds[0 + 17];
//        currentResult = (shoulder_x + height_threshold) >= elbow_x;
//        if(lastPullUpResult == false && currentResult)
//        {
//            lastPullUpResult = true;
//            return true;
//        }
//        else if(lastPullUpResult == true && currentResult)
//        {
//            return false;
//        }
//        else if((!currentResult) && (fucking > 50))  //手伸直了才能算做完一个
//        {
//            lastPullUpResult = false;
//            return false;
//        }
//        else
//        {
//            return false;
//        }
//    }
//}

//判断俯卧撑是否标准（肩部与手肘关键点的位置），这里需要改，因为平板的话大小、帧率啥都不一样了，全都要以实物为准
static bool is_pushup_standard(const float* preds,float height_threshold,int direction)
{
    static bool lastPullUpResult = false;
    static float dis_noise_elbow = 0;
    static float noise_y = 0;
    static float elbow_y = 0;
    static bool currentResult = false;

    static bool flag_yuandian = 0;
    static bool flag_shangla = 0;
    static bool flag_yicijieshu = 0;
    static int flag = 0;
    static std::vector<int> distance;
    static bool up = 0;
    static bool down = 0;
    static int i = 0;

    //没必要区分左右了，直接规定测试者头朝右边，确保测试的规范性。
    if (direction == 1)//右边视角，头朝右
    {
        noise_y = preds[0 + 17];
        elbow_y = preds[8 + 17];
        dis_noise_elbow = preds[8 +17] - preds[0 + 17];//鼻子与手肘的距离
        currentResult = (noise_y + height_threshold) >= elbow_y;//鼻子比手肘低认为是做完一个，这里使用鼻子是因为模型的精度问题，如果没问题的话直接使用肩即可
        if (dis_noise_elbow >= 65) {
            flag_yuandian = 1;
        }
        if (dis_noise_elbow <= 40) {
            flag_shangla = 1;
        }
        if (dis_noise_elbow >= 65 && flag_shangla == 1) {
            flag_yicijieshu = 1;
            flag_shangla = 0;
            flag = 1;
        }
        if (flag_yuandian) {
            v1.result = 8;
            if (flag_yicijieshu == 0) {
                distance.push_back(dis_noise_elbow);
            } else {
                if (flag) {
                    //下降到一定高度之后就停止上面的push，改为手动补帧，一共补5次，需要根据不同设备的帧率改这里
                    if (i < 3) {
                        distance.push_back(dis_noise_elbow);
                        i++;
                    } else {
                        i = 0;
                        flag = 0;
                    }

                } else {
                    int min_angle = *std::min_element(distance.begin(), distance.end());
                    int max_angle = *std::max_element(distance.begin(), distance.end());

                    //可否保存下最大最小值对应的帧，然后送入k近邻|MLP推理，判断上拉和下发是否满足条件，但是这样就会导致手臂下放之后在比较低的位置时，此时需要进行两帧图像的推理，来判断上拉和下放是否满足了
                    if (min_angle <= 43) {
                        //满足上拉条件
                        //v1.result = 8;
                        up = 1;
                    } else {
                        v1.result = 2;//下放不够下
                        up = 0;
                    }
                    if (max_angle >= 70) {
                        //v1.result = 8;
                        down = 1;
                    } else {
                        v1.result = 3;//上行时手臂不够直
                        down = 0;
                    }
                    flag_yicijieshu = 0;
                    distance.clear();
                    if (up == 1 && down == 1) {
                        up = 0;
                        down = 0;
                        v1.result = 8;
                        return true;
                    }
                }
            }
        }
    }
    else//左边视角，头朝左
    {
        noise_y = preds[0 + 17];
        elbow_y = preds[7 +17];
        dis_noise_elbow = preds[7 +17] - preds[0 + 17];
        currentResult = (noise_y + height_threshold) >= elbow_y;
        if(lastPullUpResult == false && currentResult)
        {
            lastPullUpResult = true;
            return true;
        }
        else if(lastPullUpResult == true && currentResult)
        {
            return false;
        }
        else if((!currentResult) && (dis_noise_elbow > 50))  //手伸直了才能算做完一个
        {
            lastPullUpResult = false;
            return false;
        }
        else
        {
            return false;
        }

    }
    return false;
}

//函数：检查引体向上是否标准,可以根据关键点来判断是否做完一个完整的引体向上，而不是使用延时
//bool Inference::ispullupStandard(const float* preds) {
//    static int stardard_count = 0;
//    static int last_stardard_count = 0;
//
//    static bool lastPullUpResult = false;
//    static bool currentResult = false;
//    float leftWristX = preds[9];
//    float rightWristX = preds[10];
//    float leftShoulderX = preds[5];
//    float rightShoulderX = preds[6];
//
//    // 计算手腕的宽度和肩膀的宽度
//    float wristWidth = fabs(leftWristX - rightWristX);
//    float shoulderWidth = fabs(leftShoulderX - rightShoulderX);
//
//    //preds 数组的每个关键点坐标被存储为连续的浮点数，其中第一个元素是 x 坐标，紧接着的第二个元素是 y 坐标。
//    //当访问一个关键点的 y 坐标时，需要在 x 坐标的索引上加 17（因为前面有 17 个关键点的坐标，每个关键点占用 2 个数组位置，共 34 个位置）。
//
//    // 判断手腕宽度是否大于肩膀宽度
//    bool widthCondition = wristWidth > shoulderWidth;
//
//    stardard_count = calculateangle(preds[6],preds[6+17],preds[8],preds[8+17]);//判断角度比高度容错率更高，因为每个人的脖子和脸长度不一样
//    bool heightCondition = (stardard_count <=150);
//    currentResult = heightCondition && widthCondition;
//
//    if(lastPullUpResult == false && currentResult)
//    {
//        lastPullUpResult = true;
//        return true;
//    }
//    else if(lastPullUpResult == true && currentResult)
//    {
//        return false;
//    }
//    else if(lastPullUpResult == true && !currentResult)
//    {
//        if(last_stardard_count <= stardard_count + 10)//+10是容错，因为有时候识别出来的点会跳动，影响结果
//        {
//            v1.result  = 8;
//            last_stardard_count = stardard_count;
//        }
//        else//这里表示下降过程结束，开始上拉了
//        {
//            if(last_stardard_count <= 215)
//            {
//                //进入这里就表示下一个实际不计数，下落时手臂未伸直
//                v1.result  = 0;    //result=0表示引体向上手未伸直
//                last_stardard_count = 0;
//                stardard_count = 0;
//                return false;
//            }
//            else
//            {
//                lastPullUpResult = false;
//                v1.result  = 8;   //result=8表示当前没有问题，不需要播报
//                last_stardard_count = 0;
//                stardard_count = 0;
//                return false;
//            }
//        }
//    }
//    else
//    {
//        return false;
//    }
//    return false;
//}

bool Inference::ispullupStandard(const float* preds) {
    static bool flag_yuandian = 0;
    static bool flag_shangla = 0;
    static bool flag_yicijieshu = 0;

    static int angle = 0;
    static int flag = 0;

    static std::vector<int> angle_data;

    static bool up = 0;
    static bool down = 0;

    static int i = 0;

    angle = calculateangle(preds[6],preds[6+17],preds[8],preds[8+17]);
    if(angle >= 210)
    {
        flag_yuandian = 1;
    }
    if(angle <= 190)
    {
        flag_shangla = 1;
    }
    if(angle >= 220 && flag_shangla == 1)
    {
        flag_yicijieshu = 1;
        flag_shangla = 0;
        flag = 1;
    }
    if(flag_yuandian)
    {
        v1.result = 8;
        if(flag_yicijieshu == 0)
        {
            angle_data.push_back(angle);
        }
        else {
            if (flag) {
                //下降到一定高度之后就停止上面的push，改为手动补帧，一共补5次，需要根据不同设备的帧率改这里
                if (i < 5) {
                    angle_data.push_back(angle);
                    i++;
                } else {
                    i = 0;
                    flag = 0;
                }

            } else {
                //这里面找出最大最小值即是一次完整的引体向上，之后可以将一次完整运动的数据送入神经网络或者机器学习算法中进行判断等
                int min_angle = *std::min_element(angle_data.begin(), angle_data.end());
                int max_angle = *std::max_element(angle_data.begin(), angle_data.end());

                if (min_angle <= 120) {
                    //满足上拉条件
                    up = 1;
                } else {
                    v1.result = 1;//result = 1  表示上一个引体上拉时下巴未过杆，上一个不计数
                    up = 0;
                }
                if (max_angle >= 243) {
                    //满足下放条件
                    down = 1;
                } else {
                    if(up == 0)
                    {
                        v1.result = 2;//result = 2  表示下巴未过杆并且手臂未伸直
                    }
                    else
                    {
                        v1.result = 0;//result = 0  表示上一个引体下落时手臂未伸直，上一个不计数
                    }
                    down = 0;
                }
                flag_yicijieshu = 0;
                angle_data.clear();
                if (up == 1 && down == 1) {
                    up = 0;
                    down = 0;
                    v1.result = 8;
                    return true;
                }
            }
        }
    }
    return false;
}

//测试用
static int lookpoint(const float* preds)
{
    int noseHeight = preds[7 + 17];
    //int rightwristHeight = preds[10 + 17];
    return noseHeight;
}

Inference::Inference(){
    blob_pool_allocator.set_size_compare_ratio(0.f);
    workspace_pool_allocator.set_size_compare_ratio(0.f);
}

int Inference::loadNcnnNetwork(AAssetManager* mgr, const char* modeltype , const int& modelInputShape, const float* meanVals, const float* normVals, bool useGpu) {
    if (!mgr) {
        __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "Invalid AssetManager");
        return -1;
    }

    modelShape = modelInputShape;
    gpuEnabled = useGpu;

    net.clear();
    blob_pool_allocator.clear();
    workspace_pool_allocator.clear();

    ncnn::set_cpu_powersave(2);
    ncnn::set_omp_num_threads(ncnn::get_big_cpu_count());

    net.opt = ncnn::Option();
    net.opt.use_vulkan_compute = useGpu;
    net.opt.num_threads = ncnn::get_big_cpu_count();
    net.opt.blob_allocator = &blob_pool_allocator;
    net.opt.workspace_allocator = &workspace_pool_allocator;

    char parampath[256];
    char modelpath[256];
    sprintf(parampath, "yolov11n_pose.param");
    sprintf(modelpath, "yolov11n_pose.bin");

//    if (!net.load_param(mgr, parampath) || !net.load_model(mgr, modelpath)) {
//        //LOGE("Failed to load model");
//        __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "Failed to load model");
//        return -1;
//    }
    net.load_param(mgr, parampath);
    net.load_model(mgr, modelpath);

    v1.count.assign({0, 0, 0, 0, 0});
    v1.result = 8;// 默认给8，当引体下拉未伸直时置1，其它等待新设备调试

    memcpy(this->meanVals, meanVals, 3 * sizeof(float));
    memcpy(this->normVals, normVals, 3 * sizeof(float));
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "yolov11n-pose.bin load success");

    return 0;
}

std::vector<Pose> Inference::runInference(const cv::Mat &input)
{
    cv::Mat modelInput = input;
    int imgWidth = modelInput.cols;
    int imgHeight = modelInput.rows;

    int w = imgWidth;
    int h = imgHeight;
    float scale = 1.f;
    if (w > h) {
        scale = (float)modelShape / w;
        w = modelShape;
        h = (int)(h * scale);
    }
    else {
        scale = (float)modelShape / h;
        h = modelShape;
        w = (int)(w * scale);
    }

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(modelInput.data, ncnn::Mat::PIXEL_BGR2RGB, imgWidth, imgHeight, w, h);

    int wpad = (modelShape + MAX_STRIDE - 1) / MAX_STRIDE * MAX_STRIDE - w;
    int hpad = (modelShape + MAX_STRIDE - 1) / MAX_STRIDE * MAX_STRIDE - h;

    int top = hpad / 2;
    int bottom = hpad - hpad / 2;
    int left = wpad / 2;
    int right = wpad - wpad / 2;

    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, top, bottom, left, right, ncnn::BORDER_CONSTANT, 114.f);

    in_pad.substract_mean_normalize(meanVals, normVals);

    ncnn::Extractor ex = net.create_extractor();

    ex.input("in0", in_pad);

    ncnn::Mat out;
    ex.extract("out0", out);

    // yolov8 has an output of shape (batchSize, 56,  8400) (COCO_POSE_POINT_NUM x point[x,y,prop] + prop + box[x,y,w,h])
    cv::Mat output(out.h, out.w, CV_32FC1, out.data);
    cv::transpose(output, output);
    std::cout<<output.rows << output.cols << output.channels()<<std::endl;
    float* data = (float*)output.data;


    std::vector<float>  confidences;
    std::vector<cv::Rect> boxes;
    std::vector<std::vector<float>> keyPoints;

    int rows = output.rows;
    int dimensions = output.cols;
    for (int row = 0; row < rows; row++) {
        float score = *(data + 4);
        if (score > modelScoreThreshold) {
            confidences.push_back(score);

            float x = data[0];
            float y = data[1];
            float w = data[2];
            float h = data[3];

            int left = int((x - 0.5 * w));
            int top = int((y - 0.5 * h));

            int width = int(w);
            int height = int(h);

            boxes.push_back(cv::Rect(left, top, width, height));

            std::vector<float> kps((data + 5), data + 5+COCO_POSE_POINT_NUM * 3);
            keyPoints.push_back(kps);
        }
        data += dimensions;
    }
    std::vector<int> nms_result;
    my_nms_boxes(boxes, confidences, modelScoreThreshold, modelNMSThreshold, nms_result);


    std::vector<Pose> poses;
    for (int i = 0; i < nms_result.size(); ++i) {
        int idx = nms_result[i];

        float confidence = confidences[idx];

        cv::Rect box = { int(((boxes[idx].x - int(wpad / 2)) / scale)),
                         int(((boxes[idx].y - int(hpad / 2))) / scale),
                         int(boxes[idx].width / scale),
                         int(boxes[idx].height / scale) };
        std::vector<float> kps;
        for (int j = 0; j < keyPoints[idx].size()/3; j++) {
            kps.push_back((keyPoints[idx][3 * j + 0] - int(wpad / 2)) / scale);
            kps.push_back((keyPoints[idx][3 * j + 1] - int(hpad / 2)) / scale);
            kps.push_back(keyPoints[idx][3 * j + 2]);
        };
        Pose pose;
        pose.box = box;
        pose.confidence = confidence;
        pose.kps = kps; //{ confidence, box, kps };
        poses.push_back(pose);
    }


    return poses;
}

struct value_result Inference::draw(cv::Mat& rgb, const std::vector<Pose>& objects,int selectmode) {
    static bool isStandardPullup = 0;
    static bool isStandardPushup = 0;
    static int id = 0;

    cv::Mat res = rgb;
    for (auto& obj : objects) {
        //cv::rectangle(res, obj.box, { 0, 0, 255 }, 2);

        int x = (int)obj.box.x;
        int y = (int)obj.box.y + 1;

        if (y > res.rows)
            y = res.rows;

        auto& kps = obj.kps;
        for (int k = 0; k < COCO_POSE_POINT_NUM + 2; k++) {
            if (k < COCO_POSE_POINT_NUM) {
                int kps_x = (int)std::round(kps[k * 3]);
                int kps_y = (int)std::round(kps[k * 3 + 1]);
                float kps_s = kps[k * 3 + 2];
                if (kps_s > 0.4f) {
                    cv::Scalar kps_color = cv::Scalar(KPS_COLORS[k][0], KPS_COLORS[k][1], KPS_COLORS[k][2]);
                    cv::circle(res, { kps_x, kps_y }, 5, kps_color, -1);
                }
            }
            auto& ske = SKELETON[k];
            int pos1_x = (int)std::round(kps[(ske[0] - 1) * 3]);
            int pos1_y = (int)std::round(kps[(ske[0] - 1) * 3 + 1]);

            int pos2_x = (int)std::round(kps[(ske[1] - 1) * 3]);
            int pos2_y = (int)std::round(kps[(ske[1] - 1) * 3 + 1]);

            float pos1_s = kps[(ske[0] - 1) * 3 + 2];
            float pos2_s = kps[(ske[1] - 1) * 3 + 2];

            if (pos1_s > 0.5f && pos2_s > 0.5f) {
                cv::Scalar limb_color = cv::Scalar(LIMB_COLORS[k][0], LIMB_COLORS[k][1], LIMB_COLORS[k][2]);
                cv::line(res, { pos1_x, pos1_y }, { pos2_x, pos2_y }, limb_color, 2);
            }
        }
    }

    if(objects.empty())
    {
        //如果没检测到目标啥也不干
    }
    else {
        float *preds = new float[objects.size() * 17 * 2];
        int x_index = 0; // 用于追踪preds中x坐标的索引位置,前17个填充为x
        int y_index = objects.size() * 17; // 用于追踪preds中y坐标的索引位置，后17个填充为y

        for (const auto &obj: objects) {
            for (size_t i = 0; i < obj.kps.size(); i += 3) {
                preds[x_index++] = obj.kps[i];   // 填充x坐标
                preds[y_index++] = obj.kps[i + 1]; // 填充y坐标
                // 跳过置信度
            }
        }

        //选择模式判断
        switch (selectmode) {
            case 0:
                isStandardPullup = ispullupStandard(preds);
                if (isStandardPullup) {
                    v1.count[0]++;
                }
                break;
            case 1:
                if(preds[0] > preds[16])  //头朝右侧
                {
                    if(is_body_straight(preds,1,1))
                    {
                        if(isStandardPushup = is_pushup_standard(preds,30,1))
                        {
                            v1.count[1]++;
                        }
                    }
                }
                else if(preds[0] < preds[16])
                {
                    if(is_body_straight(preds,1,0))
                    {
                        if(isStandardPushup = is_pushup_standard(preds,25,0))
                        {
                            v1.count[1]++;
                        }
                    }
                }
                break;
            case 2:
                if(preds[0] > preds[16])//头朝右
                {
                    if(is_crunch_standard(preds,0,0))
                    {
                        v1.count[2]++;
                    }
                }
                else if(preds[0] < preds[16])
                {
                    if(is_crunch_standard(preds,0,1))
                    {
                        v1.count[2]++;
                    }
                }
                break;
            case 3:

                if(is_situp_standard(preds,0,1))
                {
                    v1.count[3]++;
                }
                break;
            case 4:
                //如果使用MLP来分类的话需要用到下面这些来制作数据集
                v1.count[4] = calculateangle(preds[6],preds[6+17],preds[8],preds[8+17]);//左边手腕、手肘、肩膀的角度

                //v1.count[4] = calculate_angle(preds[15],preds[15+17],preds[13],preds[13+17],preds[11],preds[11+17]);
                break;
            default:
                break;
        }
        delete[] preds;
    }

    switch (selectmode) {
        case 0:
            v1.count[1] = 0;
            v1.count[2] = 0;
            v1.count[3] = 0;
            v1.count[4] = 0;
            return v1;
            break;
        case 1:
            v1.count[0] = 0;
            v1.count[2] = 0;
            v1.count[3] = 0;
            v1.count[4] = 0;
            return v1;
            break;
        case 2:
            v1.count[0] = 0;
            v1.count[1] = 0;
            v1.count[3] = 0;
            v1.count[4] = 0;
            return v1;
            break;
        case 3:
            v1.count[0] = 0;
            v1.count[1] = 0;
            v1.count[2] = 0;
            v1.count[4] = 0;
            return v1;
            break;
        case 4:
            v1.count[0] = 0;
            v1.count[1] = 0;
            v1.count[2] = 0;
            v1.count[3] = 0;
            //v1.count[4] = 0;
            return v1;
            break;
        default:
            return v1;
            break;
    }
}
