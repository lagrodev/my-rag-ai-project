package ai.chat.service;

import java.util.UUID;

public interface DocumentStatusPort
{


    void updateStatus(UUID fileAssetId, String status, String message);
}
