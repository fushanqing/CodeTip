# CodeTip
代码搜集推荐
## 源代码分析推荐流程
获得推荐版本坐标->下载推荐版本源代码&class->分析源代码&class
1. 构建镜像仓库的路径&maven-metadata-public.xml文件
  1. 沿着根节点开始搜索,直到搜索到maven-metadata-public.xml或者maven-metadata.xml(两者同时存在时,以public为准)
  2. 获得到达路径,如果这个路径原先不存在则构建路径;读取maven-metadata-public.xml文件中的lastUpdate时间,看看是不是比当前版本要新.如果要新则更新当前的版本
2. 通过maven-metadata-public.xml的文件的内容获得该坐标下所有的发布记录
选择推荐的版本/或者所有版本,下载jar和source文件
    