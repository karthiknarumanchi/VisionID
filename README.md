# VisionID

This project uses Google's TensorFlow Machine Learning library for image recognition using the Inception model to classify images.
Images can be both captured or chosen from the device for recognition.

![alt text](/screens/Screenshot.png?raw=true "Screenshot")

## Pre-requisites

* Android 5.0 Lollipop(API 21) or higher.
* JNI Files have to be compiled from the TensorFlow source by using Bazel. After the files have been compiled,place the '.so' files in a folder named 'jniLibs' in 'VisionID/src/main/'.

## License

    Copyright 2015, Karthik Narumanchi.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
