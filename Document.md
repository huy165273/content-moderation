Request Interface
Encoding
UTF-8 character set encoding

Request Method
POST

Recommended Timeout
1s

Request
Request URL
Cluster	Request URL	Supported Product List
Beijing	http://api-text-bj.fengkongcloud.com/text/v4	Chinese
Shanghai	http://api-text-sh.fengkongcloud.com/text/v4	Chinese
USA	http://api-text-fjny.fengkongcloud.com/text/v4	Chinese、International
Singapore	http://api-text-xjp.fengkongcloud.com/text/v4	Chinese、International
Request Parameters
Parameter Name	Type	Parameter Description	Required	Specification
accessKey	string	Authentication key for the API	Y	View details in the attachment of the account activation email.
appId	string	Application identifier	Y	Used to distinguish applications. Contact DeepCleer for activation and use the provided value.see email for details.
eventId	string	Event identifier	Y	Contact DeepCleer for activation and use the provided value.see email for details.
type	string	Type of risk detection	Y
+	data	json_object	Content of the request data	Y	Maximum length: 1MB
     kbType	string	Knowledge base type	N	The knowledge base supports a maximum input length of 510 characters. If exceeded, the text in this request will not match the knowledge base. Contact DeepCleer Business for activation. Optional values: PKB: Enable political knowledge base functionality
     translationTargetLang	string	Translation target language	N	Translate the input text into the target language. Contact DeepCleer Business for activation. Optional values: zh: Chinese en: English
     Response
     Return Parameters
     All parameters below, except for code, message, and requestId, are required when code returns 1100.

Parameter Name	Type	Parameter Description	Required	Specification
code	int	Return code	Y	1100: Success 1901: QPS limit exceeded 1902: Invalid parameters 1903: Service failure 1905: Character limit exceeded 9101: Unauthorized operation
message	string	Description of the return code	Y	Corresponds to the code: Success, QPS limit exceeded, Invalid parameters, Service failure, Character limit exceeded, Unauthorized operation
requestId	string	Request identifier	Y	A unique identifier for this request data, used for troubleshooting and effect optimization. It is strongly recommended to save this.
riskLevel	string	Disposal recommendation	Y	Possible return values: PASS: Normal, recommended for direct approval REVIEW: Suspicious, recommended for manual review REJECT: Violation, recommended for direct blocking
riskLabel1	string	Primary risk label	Y	Primary risk label. Returns 'normal' when riskLevel is PASS.
riskLabel2	string	Secondary risk label	Y	Secondary risk label. Empty when riskLevel is PASS.
riskLabel3	string	Tertiary risk label	Y	Tertiary risk label. Empty when riskLevel is PASS.
riskDescription	string	Risk reason	Y	Returns 'normal' when riskLevel is PASS.
+	riskDetail	json_object	Mapped risk details	Y
+	tokenLabels	json_object	Auxiliary information	Y	Account risk profile label information. See details below.
+	auxInfo	json_object	Auxiliary information	Y
+	allLabels	json_array	Auxiliary information	Y	All matched risk labels and detailed information.
+	businessLabels	json_array	Auxiliary information	Y	All matched business labels and detailed information.
+	tokenProfileLabels	json_array	Auxiliary information	N	Attribute account labels.
     tokenRiskLabels	json_array	Auxiliary information	N	Risk account labels.
+	langResult	json_object	Language information	N
+	kbDetail	json_object	Knowledge base details	N
     finalResult	int	Whether the result is final	Y	Value 1: The result can be used directly for disposal, distribution, and other downstream scenarios. Value 0: The result is a process result from DeepCleer's risk control and requires further manual review before being sent back to you.
     resultType	int	Whether the current result is from machine review or human review	Y	0: Machine review, 1: Human review
+	disposal	json_object	Disposal and mapping results	N	DeepCleer can return results according to your label system and identifiers. If no custom label system is configured, this field will not be returned.
     When the lang field is set to 'zh' or detected as Chinese with 'auto', the primary labels are as follows:
     Primary Label	Primary Identifier	Type	Remark
     Political	politics	Regulatory Label	type value is TEXTRISK
     Violence	violence	Regulatory Label	type value is TEXTRISK
     Pornography	porn	Regulatory Label	type value is TEXTRISK
     Banned	ban	Regulatory Label	type value is TEXTRISK
     Abuse	abuse	Regulatory Label	type value is TEXTRISK
     Advertising Law	ad_law	Regulatory Label	type value is TEXTRISK
     Advertising	ad	Regulatory Label	type value is TEXTRISK
     Blacklist	blacklist	Regulatory Label	type value is TEXTRISK
     Meaningless	meaningless	Regulatory Label	type value is TEXTRISK
     Privacy	privacy	Regulatory Label	type value is TEXTRISK
     Fraud	fraud	Regulatory Label	type value is FRAUD
     Minor	minor	Regulatory Label	type value is TEXTMINOR
     When the language is not Chinese, the primary labels are as follows:
     Primary Label	Primary Identifier	Type	Remark
     Political	Politics	Regulatory Label	type value is TEXTRISK
     Violence	Violence	Regulatory Label	type value is TEXTRISK
     Pornography	Erotic	Regulatory Label	type value is TEXTRISK
     Banned	Prohibit	Regulatory Label	type value is TEXTRISK
     Abuse	Abuse	Regulatory Label	type value is TEXTRISK
     Advertising	Ads	Regulatory Label	type value is TEXTRISK
     Blacklist	Blacklist	Regulatory Label	type value is TEXTRISK
     Example
     Request Example
     {
     "accessKey": "*************",
     "appId": "default",
     "eventId": "text",
     "type": "TEXTRISK",
     "data": {
     "text": "Add me on QQ: qq12345",
     "tokenId": "4567898765jhgfdsa",
     "ip": "118.89.214.89",
     "deviceId": "*************",
     "nickname": "***********",
     "extra": {
     "topic": "12345",
     "atId": "username1",
     "room": "ceshi123",
     "receiveTokenId": "username2"
     }
     }
     }
     Response Example
     {
     "allLabels": [
     {
     "probability": 1,
     "riskDescription": "Political: Political: Political",
     "riskDetail": {},
     "riskLabel1": "politics",
     "riskLabel2": "political",
     "riskLabel3": "political",
     "riskLevel": "REVIEW"
     },
     {
     "probability": 0.95559550232975,
     "riskDescription": "Advertising: Add friend: Add friend",
     "riskDetail": {
     "matchedLists": [
     {
     "name": "Community Sensitive Word List",
     "words": [
     {
     "position": [
     6,
     7
     ],
     "word": "qq"
     }
     ]
     }
     ]
     },
     "riskLabel1": "ad",
     "riskLabel2": "add_friend",
     "riskLabel3": "add_friend",
     "riskLevel": "REJECT"
     },
     {
     "probability": 1,
     "riskDescription": "Advertising: Contact information: Contact information",
     "riskDetail": {},
     "riskLabel1": "ad",
     "riskLabel2": "contact_info",
     "riskLabel3": "contact_info",
     "riskLevel": "REJECT"
     }
     ],
     "auxInfo": {
     "contactResult": [
     {
     "contactString": "qq12345",
     "contactType": 2
     }
     ],
     "filteredText": "Add me on QQ: **12345"
     },
     "businessLabels": [],
     "code": 1100,
     "message": "Success",
     "finalResult": 1,
     "resultType": 0,
     "requestId": "bb917ec5fa11fd02d226fb384968feb1",
     "riskDescription": "Advertising: Contact information: Contact information",
     "riskDetail": {},
     "riskLabel1": "ad",
     "riskLabel2": "contact_info",
     "riskLabel3": "contact_info",
     "riskLevel": "REJECT"
     }
