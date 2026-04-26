%% ==========================================================
% TWO PHASE METHOD (Same format as your simplex code)
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

%% ----------------------------------------------------------
% PHASE I
% Variables:
% x1 x2 s1 s3 a1 a2
%% ----------------------------------------------------------

C1 = [0 0 0 0 -1 -1];   % Max(-a1-a2)

info = [1 1 -1 0 1 0;
        1 2  0 0 0 1;
        2 1  0 1 0 0];

b = [4;6;8];

A = [info b];

Cost = [C1 0];

% Initial Basic Variables = a1,a2,s3
BV = [5 6 4];

% Initial Zj-Cj
ZRow = Cost(BV)*A - Cost;

fprintf('=========== PHASE I ===========\n')

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
        
        disp(array2table([ZRow;A]))
        
    else
        Run = false;
    end
    
end

W = -ZRow(end);

fprintf('Phase I optimum W = %f\n',W)

if abs(W) > 1e-6
    fprintf('Problem is infeasible\n')
    return
end

%% ----------------------------------------------------------
% PHASE II
% Remove artificial columns a1,a2
%% ----------------------------------------------------------

fprintf('\n=========== PHASE II ===========\n')

A(:,[5 6]) = [];

% New variables:
% x1 x2 s1 s3

C2 = [-2 -3 0 0];   % Max(-2x1-3x2)

Cost = [C2 0];

% Update BV after removing columns
for i=1:length(BV)
    
    if BV(i)==5 || BV(i)==6
        % If artificial still in basis, replace manually
        BV(i)=1;
    elseif BV(i)>6
        BV(i)=BV(i)-2;
    elseif BV(i)>4
        BV(i)=BV(i)-2;
    end
    
end

ZRow = Cost(BV)*A - Cost;

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
        
        disp(array2table([ZRow;A]))
        
    else
        Run = false;
    end
    
end

disp('Optimal Solution')

BFS = zeros(1,size(A,2));
BFS(BV)=A(:,end);
BFS(end)=sum(BFS.*Cost);

disp(BFS)

fprintf('Minimum Z = %f\n',-BFS(end))
