%% ==========================================================
% BIG M METHOD (Same format as your simplex code)
% Min Z = 2x1 + 3x2
% s.t.
% x1 + x2 >= 4
% x1 + 2x2 = 6
% 2x1 + x2 <= 8
% x1,x2 >= 0
%% ==========================================================

clc
clear all
format short

M = 1000;

% Convert to Max form:
% Max (-2x1 -3x2 -M a1 -M a2)

C = [-2 -3 0 0 -M -M];

% Variables:
% x1 x2 s1 s3 a1 a2

info = [1 1 -1 0 1 0;
        1 2  0 0 0 1;
        2 1  0 1 0 0];

b = [4;6;8];

A = [info b];

NOVariables = 2;

% Cost row
Cost = [C 0];

% Initial Basic Variables = a1,a2,s3
BV = [5 6 4];

% Compute Zj-Cj
ZRow = Cost(BV)*A - Cost;

ZjCj = [ZRow;A];
disp(array2table(ZjCj))

Run = true;

while Run
    
    if any(ZRow(1:end-1) < 0)
        
        ZC = ZRow(1:end-1);
        [EnterCol,Pvt_Col] = min(ZC);
        
        sol = A(:,end);
        Column = A(:,Pvt_Col);
        
        ratio = inf(size(Column));
        
        for i=1:length(Column)
            if Column(i)>0
                ratio(i)=sol(i)/Column(i);
            end
        end
        
        [MinRatio,Pvt_Row] = min(ratio);
        
        BV(Pvt_Row)=Pvt_Col;
        
        Pvt_Key = A(Pvt_Row,Pvt_Col);
        A(Pvt_Row,:) = A(Pvt_Row,:)/Pvt_Key;
        
        for i=1:size(A,1)
            if i~=Pvt_Row
                A(i,:) = A(i,:) - A(i,Pvt_Col)*A(Pvt_Row,:);
            end
        end
        
        ZRow = ZRow - ZRow(Pvt_Col)*A(Pvt_Row,:);
        
        ZjCj = [ZRow;A];
        disp(array2table(ZjCj))
        
    else
        Run=false;
    end
    
end

disp('Optimal Solution')

BFS = zeros(1,size(A,2));
BFS(BV)=A(:,end);
BFS(end)=sum(BFS.*Cost);

disp(BFS)
fprintf('Minimum Z = %f\n',-BFS(end))
