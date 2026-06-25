2. 输入参数
以下请求参数列表仅列出了接口请求参数和部分公共参数，完整公共参数列表见 公共请求参数。

参数名称	必选	类型	描述
Action	是	String	公共参数，本接口取值：TextModeration。
Version	是	String	公共参数，本接口取值：2020-12-29。
Region	是	String	公共参数，详见产品支持的 地域列表。
Content	是	String	待检测的文本内容，需为UTF-8编码并以Base64格式传入。

示例值：5L2g55qE5Lil6LCo6K6p5L2g5Y+R546w77yM5Lqn5ZOB57uP55CG5Y+r5YmR6Z2S
BizType	否	String	接口使用的识别策略编号，需在控制台获取。详细获取方式请参考以下链接：
- 内容安全（详见步骤四：策略配置）：点击这里
- AI生成识别（详见服务对接->方式二）：点击这里
示例值：TencentCloudDefault
DataId	否	String	该字段表示您为待检测文本分配的数据ID，作用是方便您对数据进行标识和管理。
取值：可由英文字母、数字、四种特殊符号（_，-，@，#）组成，长度不超过64个字符。
示例值：a6127dd-c2a0-43e7-a3da-d27022d39ba7
User	否	User	该字段标识用户信息，传入后可增强甄别有违规风险的发布者账号。
Device	否	Device	该字段标识设备信息，传入后可增强甄别有违规风险的发布者设备。
SourceLanguage	否	String	Content字段的原始语种，枚举值包括 zh 和 en：
- 推荐使用 zh
- en 适用于纯英文内容，耗时较高。若需使用 en，请先通过反馈工单确认

示例值：zh
Type	否	String	服务类型，枚举值包括 TEXT 和 TEXT_AIGC：
TEXT：内容安全
TEXT_AIGC：AI生成识别
示例值：TEXT
SessionId	否	String	适用于上下文关联审核场景，若多条文本内容需要联合审核，通过该字段关联会话。
示例值：7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b
3. 输出参数
参数名称	类型	描述
BizType	String	该字段用于回显检测对象请求参数中的 BizType，与输入的 BizType 值对应。
示例值：TencentCloudDefault
Suggestion	String	用于标识对本次请求的处置建议，共三种返回值。
返回值：Block: 建议直接做违规处置，Review: 建议人工二次确认，Pass: 未识别到风险。
示例值：Block
Label	String	该字段用于返回检测结果（DetailResults）中所对应的优先级最高的恶意标签，表示模型推荐的审核结果，建议您按照业务所需，对不同违规类型与建议值进行处理。
返回值：Normal：正常，Porn：色情，Abuse：谩骂，Ad：广告；以及其他令人反感、不安全或不适宜的内容类型
示例值：Porn
SubLabel	String	对应 Label 字段下的二级子标签，表示该 Label 下更细分的违规点。
示例值：SexualBehavior（该值为 Porn 下的一个二级标签）
Score	Integer	该字段标识 SubLabel 的置信度，取值范围为 0 - 100，值越高代表置信度越高。
示例值：85
Keywords	Array of String	该字段标识被检测文本所命中的关键词，可能返回0个或多个关键词。
注意：此字段可能返回 null，表示取不到有效值。
示例值：["优惠券", "线下兑换"]
DetailResults	Array of DetailResults	该字段返回的检测的详细信息，返回值信息可参阅对应数据结构 DetailResults 的详细描述。
注意：此字段可能返回 null，表示取不到有效值。
RiskDetails	Array of RiskDetails	该字段标识入参 User 的检测结果，具体内容参阅数据结构 RiskDetails。
注意：此字段可能返回 null，表示取不到有效值。
Extra	String	该字段用于返回根据您的需求配置的附加信息（Extra），如未配置则默认返回值为空。
备注：不同客户或Biztype下返回信息不同，如需配置该字段请提交工单咨询或联系售后专员处理。
示例值：{ad}
DataId	String	该字段用于回显检测对象请求参数中的 DataId，与输入的 DataId 值对应。
示例值：a6127dd-c2a0-43e7-a3da-d27022d39ba7
ContextText	String	历史上下文关联的字段，不再推荐使用。上下文关联审核可通过入参的 SessionId 来实现。
示例值：优惠券
SentimentAnalysis	SentimentAnalysis	该字段为历史结构字段，不再推荐使用。
注意：此字段可能返回 null，表示取不到有效值。
HitType	String	该字段为历史结构字段，不再推荐使用。
示例值：text_nlp_tianji
SessionId	String	该字段用于回显检测对象请求参数中的 SessionId，与输入的 SessionId 值对应。
示例值：7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b
RequestId	String	唯一请求 ID，由服务端生成，每次请求都会返回（若请求因其他原因未能抵达服务端，则该次请求不会获得 RequestId）。定位问题时需要提供该次请求的 RequestId。
4. 示例
示例1 文本内容安全
输入示例
POST / HTTP/1.1
Host: tms.tencentcloudapi.com
Content-Type: application/json
X-TC-Action: TextModeration
<公共请求参数>

{
    "Content": "5Yqg5oiR5aW95Y+LIOe7meS9oOS8mOaDoOWIuA==",
    "BizType": "TencentCloudDefault"
}
输出示例
{
    "Response": {
        "RequestId": "d636333a-0d14-4962-8287-e6e8af0a10f2",
        "BizType": "TencentCloudDefault",
        "Label": "Ad",
        "SubLabel": "",
        "Suggestion": "Block",
        "Keywords": [
            "优惠券"
        ],
        "Score": 100,
        "DataId": "CSFb_MJRV5piaczW",
        "DetailResults": [
            {
                "Label": "Polity",
                "SubLabel": "",
                "Suggestion": "Pass",
                "Keywords": [],
                "Score": 0,
                "LibType": 0,
                "LibId": "",
                "LibName": "",
                "Tags": null,
                "HitInfos": []
            },
            {
                "Label": "Ad",
                "SubLabel": "",
                "Suggestion": "Block",
                "Keywords": [
                    "优惠券"
                ],
                "Score": 100,
                "LibType": 2,
                "LibId": "",
                "LibName": "",
                "Tags": null,
                "HitInfos": [
                    {
                        "Type": "Keyword",
                        "Keyword": "优惠券",
                        "LibName": "default_1_0_1256309736_100004528167",
                        "Positions": [
                            {
                                "Start": 7,
                                "End": 10
                            }
                        ]
                    }
                ]
            },
            {
                "Label": "Abuse",
                "SubLabel": "",
                "Suggestion": "Pass",
                "Keywords": [],
                "Score": 0,
                "LibType": 0,
                "LibId": "",
                "LibName": "",
                "Tags": null,
                "HitInfos": []
            },
            {
                "Label": "Illegal",
                "SubLabel": "",
                "Suggestion": "Pass",
                "Keywords": [],
                "Score": 0,
                "LibType": 0,
                "LibId": "",
                "LibName": "",
                "Tags": null,
                "HitInfos": []
            },
            {
                "Label": "Terror",
                "SubLabel": "",
                "Suggestion": "Pass",
                "Keywords": [],
                "Score": 0,
                "LibType": 0,
                "LibId": "",
                "LibName": "",
                "Tags": null,
                "HitInfos": []
            },
            {
                "Label": "Porn",
                "SubLabel": "",
                "Suggestion": "Pass",
                "Keywords": [],
                "Score": 2,
                "LibType": 0,
                "LibId": "",
                "LibName": "",
                "Tags": null,
                "HitInfos": []
            }
        ],
        "RiskDetails": null,
        "Extra": "",
        "ContextText": "",
        "SentimentAnalysis": {}
    }
}
5. 开发者资源
腾讯云 API 平台
腾讯云 API 平台 是综合 API 文档、错误码、API Explorer 及 SDK 等资源的统一查询平台，方便您从同一入口查询及使用腾讯云提供的所有 API 服务。

API Inspector
用户可通过 API Inspector 查看控制台每一步操作关联的 API 调用情况，并自动生成各语言版本的 API 代码，也可前往 API Explorer 进行在线调试。

SDK
云 API 3.0 提供了配套的开发工具集（SDK），支持多种编程语言，能更方便的调用 API。

Tencent Cloud SDK 3.0 for Python: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for Java: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for PHP: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for Go: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for Node.js: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for .NET: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for C++: CNB, GitHub, Gitee
Tencent Cloud SDK 3.0 for Ruby: CNB, GitHub, Gitee
命令行工具
Tencent Cloud CLI 3.0
6. 错误码
以下仅列出了接口业务逻辑相关的错误码，其他错误码详见 公共错误码。

错误码	描述
InternalError.ErrTextTimeOut	请求超时。
InternalError.QueryReqLimited	查询异常：请求被限流
InvalidParameter.ErrAction	错误的action。
InvalidParameter.ErrTextContentLen	请求的文本长度过长。
InvalidParameter.ErrTextContentType	文本类型错误，需要base64的文本。
InvalidParameter.ParameterError	InvalidParameter.ParameterError
InvalidParameterValue.ErrFileContent	FileContent不可用，传入的Base64编码无法转换成标准utf8内容。
InvalidParameterValue.ErrTextContentLen	请求的文本长度超过限制。
InvalidParameterValue.ErrTextContentType	请求的文本格式错误（需要base64编码格式的文本）。
InvalidParameterValue.ErrType	Type参数值不支持
RequestLimitExceeded	请求的次数超过了频率限制。
UnauthorizedOperation.Unauthorized	未开通权限/无有效套餐包/账号已欠费。